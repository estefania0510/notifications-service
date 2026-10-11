package co.andesexpress.notifications.usecase;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.port.in.GetNotificationsInputPort;
import co.andesexpress.notifications.domain.port.out.NotificationRepositoryPort;

import java.util.List;

public class GetNotificationsUseCase implements GetNotificationsInputPort {

    private final NotificationRepositoryPort repository;

    public GetNotificationsUseCase(NotificationRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public List<Notification> getByOrderId(String orderId) {
        return repository.findByOrderId(orderId);
    }
}