# Notifications Service — AndesExpress

Microservicio que avisa al cliente cuando su pedido es confirmado y cuando su guía de envío está lista. Construido con **arquitectura hexagonal** (puertos y adaptadores): el dominio no depende de Spring, de AWS ni de ningún canal de envío.

## Qué hace

- Recibe eventos de Orders (`PedidoConfirmado`, `GuiaLista`) por cola SQS o por HTTP.
- Guarda un registro de cada notificación en **DynamoDB** (AWS).
- Envía el aviso al cliente (webhook en las pruebas) y marca la notificación como `SENT` o `FAILED`.
- Es **idempotente**: si el mismo evento llega dos veces, el cliente recibe un solo aviso.

## Arquitectura

```
co.andesexpress.notifications
├── domain
│   ├── model        → Notification, NotificationType, NotificationStatus
│   └── port         → puertos de entrada y de salida (repositorio, envío)
├── usecase          → ProcessEventUseCase, GetNotificationsUseCase
├── entrypoints/web  → controller REST y DTOs
├── drivenadapters   → DynamoDB, canal de envío
└── config           → configuración de beans y cliente AWS
```

`domain` y `usecase` son Java puro, sin Spring ni AWS.

## Stack

- Java 21 y Spring Boot (misma versión que Orders)
- DynamoDB en AWS (persistencia)
- AWS SDK v2 (DynamoDB y SQS)
- SQS emulado en local con Nimbus (`andes-aws`, puerto 4566)

## Por qué DynamoDB

Las notificaciones siempre se consultan por pedido y tipo de aviso, sin cruzar tablas. Además, la escritura condicional de DynamoDB permite evitar duplicados sin transacciones entre servicios.

| Dato | Nivel de consistencia | Motivo |
|---|---|---|
| Estado del aviso (PENDING, SENT, FAILED) | Eventual | Depende de un envío asíncrono que puede tardar o fallar |
| Registro por pedido y tipo | Fuerte (por ítem) | La escritura condicional evita el aviso duplicado |

## Tabla en DynamoDB

| Atributo | Rol |
|---|---|
| `orderId` (String) | Clave de partición |
| `type` (String) | Clave de ordenación (`ORDER_CONFIRMED` o `GUIDE_READY`) |

Capacidad on-demand. El servicio **crea la tabla al arrancar** si no existe en la cuenta de las credenciales activas (`DynamoTableInitializer`), así que no hay que crearla a mano.

## Cómo levantar el proyecto

### 1. Requisitos

- Java 21 y Maven (se usa el wrapper `mvnw.cmd`)
- Docker (para el emulador de SQS)
- Una sesión activa del AWS Learner Lab

### 2. Emulador de SQS (Nimbus)

Debe estar corriendo el contenedor `andes-aws` en el puerto 4566 con la cola `pedido-confirmado-queue`. Se comprueba con:

```
docker ps
```

Si no aparece, créalo:

```
docker run --name andes-aws -p 4566:4566 -d ghcr.io/nimbus-local/nimbus:latest
```

### 3. Credenciales de AWS

Las credenciales **no están en el repositorio**. Se pasan como variables de entorno en la misma ventana donde se arranca el servicio.

1. En el Learner Lab pulsa **Start Lab** y espera el círculo verde junto a **AWS**.
2. Abre **AWS Details > AWS CLI > Show** y copia los tres valores.

PowerShell:

```powershell
$env:AWS_ACCESS_KEY_ID="..."
$env:AWS_SECRET_ACCESS_KEY="..."
$env:AWS_SESSION_TOKEN="..."
```

Las credenciales cambian cada vez que se reinicia el laboratorio. Nunca se suben al repositorio.

### 4. Arrancar

```powershell
.\mvnw.cmd clean spring-boot:run
```

El servicio queda en `http://localhost:8081`. En el log debe aparecer que la tabla `notifications` ya existe o fue creada.

### Pruebas

```
.\mvnw.cmd clean test
```

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/notifications/events` | Recibe un evento y genera la notificación |
| `GET` | `/notifications/order/{orderId}` | Lista las notificaciones de un pedido |

### Ejemplo: pedido confirmado

```bash
curl -X POST http://localhost:8081/notifications/events \
  -H "Content-Type: application/json" \
  -d '{"eventType":"PedidoConfirmado","orderId":"abc-123","customerEmail":"cliente@correo.com"}'
```

### Ejemplo: guía lista

El correo es opcional: si no viene, se toma el de la notificación `PedidoConfirmado` del mismo pedido.

```bash
curl -X POST http://localhost:8081/notifications/events \
  -H "Content-Type: application/json" \
  -d '{"eventType":"GuiaLista","orderId":"abc-123"}'
```

### Respuesta

```json
{
  "id": "9fcecf5c-b32d-413e-b8b2-673a1547379b",
  "orderId": "abc-123",
  "type": "ORDER_CONFIRMED",
  "customerEmail": "cliente@correo.com",
  "status": "SENT",
  "createdAt": "2026-10-12T22:16:19.734296100Z",
  "errorMessage": null
}
```

## Eventos soportados

| `eventType` | Tipo de notificación | Cuándo llega |
|---|---|---|
| `PedidoConfirmado` | `ORDER_CONFIRMED` | Orders confirma el pedido |
| `GuiaLista` | `GUIDE_READY` | La guía en PDF queda disponible |

Un `eventType` desconocido se rechaza con un error de validación.

## Idempotencia

Cada notificación se identifica por **pedido + tipo**. Antes de guardar, el servicio comprueba si ya existe y, además, usa una **escritura condicional** en DynamoDB para cubrir el caso de dos eventos idénticos que llegan a la vez.

- Mismo evento repetido: devuelve la notificación original (mismo `id`), no envía un segundo aviso y deja en el log `Evento duplicado ignorado`.
- Un mismo pedido puede tener dos filas: una `ORDER_CONFIRMED` y otra `GUIDE_READY`.

## Ciclo de una notificación

```
PENDING → SENT
        ↘ FAILED
```

Si el envío falla, la notificación queda en `FAILED` con el motivo en `errorMessage`, y el evento no se pierde.

## Integración con otros servicios

**Orders → Notifications (SQS):** Orders publica en `pedido-confirmado-queue` el evento `PedidoConfirmado` con `orderId` y `customerEmail`. Notifications lo consume y genera el aviso.

## Pendientes conocidos

- Definir quién publica el evento `GuiaLista` cuando la Lambda termina el PDF (el servicio ya lo soporta).
- Incluir el enlace de la guía en el aviso de guía lista.
- Cola SQS real en AWS en lugar del emulador local.