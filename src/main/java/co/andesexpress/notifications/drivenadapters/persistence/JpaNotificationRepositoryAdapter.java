package co.andesexpress.notifications.drivenadapters.persistence;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.model.NotificationType;
import co.andesexpress.notifications.domain.port.out.NotificationRepositoryPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaNotificationRepositoryAdapter implements NotificationRepositoryPort {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final SpringDataNotificationRepository jpa;

    public JpaNotificationRepositoryAdapter(SpringDataNotificationRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Guarda la notificación. Si otro evento idéntico (mismo pedido y tipo) ya la insertó
     * al mismo tiempo, la restricción única de Postgres rechaza este insert y devolvemos
     * la que ya existía (con SU id, distinto al de la que intentamos guardar).
     */
    @Override
    public Notification save(Notification notification) {
        try {
            return toDomain(jpa.saveAndFlush(toEntity(notification)));
        } catch (DataIntegrityViolationException e) {
            return jpa.findByOrderIdAndType(notification.getOrderId(), notification.getType())
                    .filter(existing -> !existing.getId().equals(notification.getId()))
                    .map(this::toDomain)
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public Optional<Notification> findByOrderIdAndType(String orderId, NotificationType type) {
        return jpa.findByOrderIdAndType(orderId, type).map(this::toDomain);
    }

    @Override
    public List<Notification> findByOrderId(String orderId) {
        return jpa.findByOrderId(orderId).stream().map(this::toDomain).toList();
    }

    private NotificationEntity toEntity(Notification n) {
        String error = n.getErrorMessage();
        if (error != null && error.length() > MAX_ERROR_LENGTH) {
            error = error.substring(0, MAX_ERROR_LENGTH);
        }
        return new NotificationEntity(n.getId(), n.getOrderId(), n.getType(),
                n.getStatus(), n.getCreatedAt(), error);
    }

    private Notification toDomain(NotificationEntity e) {
        return Notification.reconstruct(e.getId(), e.getOrderId(), e.getType(),
                e.getStatus(), e.getCreatedAt(), e.getErrorMessage());
    }
}