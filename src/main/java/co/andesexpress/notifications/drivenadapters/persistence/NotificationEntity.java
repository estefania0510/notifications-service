package co.andesexpress.notifications.drivenadapters.persistence;

import co.andesexpress.notifications.domain.model.NotificationStatus;
import co.andesexpress.notifications.domain.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notifications_order_type",
                columnNames = {"order_id", "type"}))
public class NotificationEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // Nullable a propósito: las filas viejas (antes del correo) no lo tienen.
    @Column(name = "customer_email", length = 254)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    protected NotificationEntity() {
        // requerido por JPA
    }

    public NotificationEntity(String id, String orderId, NotificationType type, String customerEmail,
                              NotificationStatus status, Instant createdAt, String errorMessage) {
        this.id = id;
        this.orderId = orderId;
        this.type = type;
        this.customerEmail = customerEmail;
        this.status = status;
        this.createdAt = createdAt;
        this.errorMessage = errorMessage;
    }

    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public NotificationType getType() { return type; }
    public String getCustomerEmail() { return customerEmail; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getErrorMessage() { return errorMessage; }
}