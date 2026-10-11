package co.andesexpress.notifications_service.usecase;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.model.NotificationStatus;
import co.andesexpress.notifications.domain.model.NotificationType;
import co.andesexpress.notifications.usecase.ProcessEventUseCase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEventUseCaseTest {

    FakeNotificationRepository repository;
    AtomicInteger envios;
    boolean senderFalla;
    ProcessEventUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeNotificationRepository();
        envios = new AtomicInteger();
        senderFalla = false;
        useCase = new ProcessEventUseCase(repository, notification -> {
            envios.incrementAndGet();
            if (senderFalla) {
                throw new RuntimeException("Canal de envío caído");
            }
        });
    }

    @Test
    void pedidoConfirmado_creaNotificacionEnviada() {
        Notification n = useCase.processEvent("PedidoConfirmado", "order-1");

        assertEquals(NotificationType.ORDER_CONFIRMED, n.getType());
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertEquals("order-1", n.getOrderId());
        assertEquals(1, envios.get());
    }

    @Test
    void guiaLista_creaNotificacionDeGuia() {
        Notification n = useCase.processEvent("GuiaLista", "order-1");

        assertEquals(NotificationType.GUIDE_READY, n.getType());
        assertEquals(NotificationStatus.SENT, n.getStatus());
    }

    @Test
    void eventoDuplicado_devuelveLaMismaNotificacionYNoEnviaDeNuevo() {
        Notification primera = useCase.processEvent("PedidoConfirmado", "order-1");
        Notification segunda = useCase.processEvent("PedidoConfirmado", "order-1");

        assertEquals(primera.getId(), segunda.getId());
        assertEquals(1, envios.get());
        assertEquals(1, repository.store.size());
    }

    @Test
    void mismoPedidoConTiposDistintos_creaDosNotificaciones() {
        useCase.processEvent("PedidoConfirmado", "order-1");
        useCase.processEvent("GuiaLista", "order-1");

        assertEquals(2, repository.findByOrderId("order-1").size());
        assertEquals(2, envios.get());
    }

    @Test
    void siElEnvioFalla_quedaFailedConElMotivo() {
        senderFalla = true;

        Notification n = useCase.processEvent("PedidoConfirmado", "order-1");

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertEquals("Canal de envío caído", n.getErrorMessage());
        assertEquals(NotificationStatus.FAILED, repository.findByOrderId("order-1").get(0).getStatus());
    }

    @Test
    void tipoDeEventoDesconocido_lanzaExcepcionYNoGuardaNada() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.processEvent("EventoRaro", "order-1"));

        assertTrue(repository.store.isEmpty());
        assertEquals(0, envios.get());
    }
}