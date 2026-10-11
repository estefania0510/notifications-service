package co.andesexpress.notifications.entrypoints.web;

import co.andesexpress.notifications.domain.model.Notification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String orderId,
        String type,
        String status,
        Instant createdAt,
        String errorMessage) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getOrderId(), n.getType().name(),
                n.getStatus().name(), n.getCreatedAt(), n.getErrorMessage());
    }
}