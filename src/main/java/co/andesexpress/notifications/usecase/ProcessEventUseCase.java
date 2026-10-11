package co.andesexpress.notifications.usecase;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.model.NotificationType;
import co.andesexpress.notifications.domain.port.in.ProcessEventInputPort;
import co.andesexpress.notifications.domain.port.out.NotificationRepositoryPort;
import co.andesexpress.notifications.domain.port.out.NotificationSenderPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ProcessEventUseCase implements ProcessEventInputPort {

    private static final Logger log = LoggerFactory.getLogger(ProcessEventUseCase.class);

    private final NotificationRepositoryPort repository;
    private final NotificationSenderPort sender;

    public ProcessEventUseCase(NotificationRepositoryPort repository, NotificationSenderPort sender) {
        this.repository = repository;
        this.sender = sender;
    }

    @Override
    public Notification processEvent(String eventType, String orderId) {
        NotificationType type = NotificationType.fromEventType(eventType);

        // Idempotencia: el mismo evento para el mismo pedido no se procesa dos veces.
        Optional<Notification> existing = repository.findByOrderIdAndType(orderId, type);
        if (existing.isPresent()) {
            log.info("Evento duplicado ignorado: orderId={}, type={}", orderId, type);
            return existing.get();
        }

        Notification created = Notification.create(orderId, type);
        Notification notification = repository.save(created);

        // Otro evento idéntico llegó primero (carrera): devolvemos esa y no enviamos de nuevo
        if (!notification.getId().equals(created.getId())) {
            log.info("Evento duplicado concurrente ignorado: orderId={}, type={}", orderId, type);
            return notification;
        }
        try {
            sender.send(notification);
            notification.markSent();
            log.info("Notificación enviada: orderId={}, type={}", orderId, type);
        } catch (RuntimeException e) {
            notification.markFailed(e.getMessage());
            log.warn("Falló el envío de la notificación: orderId={}, type={}, motivo={}",
                    orderId, type, e.getMessage());
        }
        return repository.save(notification);
    }
}