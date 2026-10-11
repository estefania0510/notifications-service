package co.andesexpress.notifications.domain.port.out;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.model.NotificationType;

import java.util.List;
import java.util.Optional;

public interface NotificationRepositoryPort {
    Notification save(Notification notification);
    Optional<Notification> findByOrderIdAndType(String orderId, NotificationType type);
    List<Notification> findByOrderId(String orderId);
}