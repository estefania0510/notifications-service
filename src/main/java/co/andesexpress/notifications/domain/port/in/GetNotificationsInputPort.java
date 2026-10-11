package co.andesexpress.notifications.domain.port.in;

import co.andesexpress.notifications.domain.model.Notification;

import java.util.List;

public interface GetNotificationsInputPort {
    List<Notification> getByOrderId(String orderId);
}