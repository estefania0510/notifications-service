package co.andesexpress.notifications.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class Notification {

    private String id;
    private final String orderId;
    private final NotificationType type;
    private final String customerEmail;
    private NotificationStatus status;
    private final Instant createdAt;
    private String errorMessage;

    private Notification(String id, String orderId, NotificationType type, String customerEmail,
                         NotificationStatus status, Instant createdAt, String errorMessage) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("El id del pedido es obligatorio");
        }
        if (type == null) {
            throw new IllegalArgumentException("El tipo de notificación es obligatorio");
        }
        this.id = id;
        this.orderId = orderId;
        this.type = type;
        this.customerEmail = customerEmail;
        this.status = status;
        this.createdAt = createdAt;
        this.errorMessage = errorMessage;
    }

    public static Notification create(String orderId, NotificationType type, String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank() || !customerEmail.contains("@")) {
            throw new IllegalArgumentException("El correo del cliente es obligatorio y debe ser válido");
        }
        return new Notification(UUID.randomUUID().toString(), orderId, type, customerEmail.trim(),
                NotificationStatus.PENDING, Instant.now(), null);
    }

    /** Reconstruye una notificación guardada (lo usa el adaptador de persistencia). */
    public static Notification reconstruct(String id, String orderId, NotificationType type, String customerEmail,
                                           NotificationStatus status, Instant createdAt, String errorMessage) {
        return new Notification(id, orderId, type, customerEmail, status, createdAt, errorMessage);
    }

    public void markSent() {
        if (status != NotificationStatus.PENDING) {
            throw new IllegalStateException("Solo una notificación pendiente puede marcarse como enviada");
        }
        this.status = NotificationStatus.SENT;
        this.errorMessage = null;
    }

    public void markFailed(String reason) {
        if (status != NotificationStatus.PENDING) {
            throw new IllegalStateException("Solo una notificación pendiente puede marcarse como fallida");
        }
        this.status = NotificationStatus.FAILED;
        this.errorMessage = reason;
    }

    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public NotificationType getType() { return type; }
    public String getCustomerEmail() { return customerEmail; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getErrorMessage() { return errorMessage; }
}