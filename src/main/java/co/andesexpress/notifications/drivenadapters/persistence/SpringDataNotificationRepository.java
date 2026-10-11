package co.andesexpress.notifications.drivenadapters.persistence;

import co.andesexpress.notifications.domain.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity, String> {

    Optional<NotificationEntity> findByOrderIdAndType(String orderId, NotificationType type);

    List<NotificationEntity> findByOrderId(String orderId);
}