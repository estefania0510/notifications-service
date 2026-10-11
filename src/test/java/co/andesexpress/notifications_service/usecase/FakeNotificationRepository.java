package co.andesexpress.notifications_service.usecase;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.model.NotificationType;
import co.andesexpress.notifications.domain.port.out.NotificationRepositoryPort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class FakeNotificationRepository implements NotificationRepositoryPort {

    final Map<String, Notification> store = new LinkedHashMap<>();

    @Override
    public Notification save(Notification notification) {
        store.put(notification.getId(), notification);
        return notification;
    }

    @Override
    public Optional<Notification> findByOrderIdAndType(String orderId, NotificationType type) {
        return store.values().stream()
                .filter(n -> n.getOrderId().equals(orderId) && n.getType() == type)
                .findFirst();
    }

    @Override
    public List<Notification> findByOrderId(String orderId) {
        return new ArrayList<>(store.values().stream()
                .filter(n -> n.getOrderId().equals(orderId))
                .toList());
    }
}