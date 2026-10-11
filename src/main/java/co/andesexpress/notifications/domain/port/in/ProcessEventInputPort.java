package co.andesexpress.notifications.domain.port.in;

import co.andesexpress.notifications.domain.model.Notification;

public interface ProcessEventInputPort {
    Notification processEvent(String eventType, String orderId, String customerEmail);
}