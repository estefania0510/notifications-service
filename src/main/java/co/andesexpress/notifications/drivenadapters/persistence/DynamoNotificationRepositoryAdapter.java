package co.andesexpress.notifications.drivenadapters.persistence;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.model.NotificationStatus;
import co.andesexpress.notifications.domain.model.NotificationType;
import co.andesexpress.notifications.domain.port.out.NotificationRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DynamoNotificationRepositoryAdapter implements NotificationRepositoryPort {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final DynamoDbClient dynamo;
    private final String table;

    public DynamoNotificationRepositoryAdapter(DynamoDbClient dynamo,
                                               @Value("${notifications.dynamodb.table:notifications}") String table) {
        this.dynamo = dynamo;
        this.table = table;
    }

    /**
     * Inserta si (orderId, type) no existe, o actualiza si existe con el MISMO id.
     * Si existe con otro id, otro evento idéntico llegó primero: devolvemos esa notificación
     * (con SU id, distinto al de la que intentamos guardar).
     */
    @Override
    public Notification save(Notification notification) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(table)
                .item(toItem(notification))
                .conditionExpression("attribute_not_exists(orderId) OR #id = :id")
                .expressionAttributeNames(Map.of("#id", "id"))
                .expressionAttributeValues(Map.of(":id", AttributeValue.fromS(notification.getId())))
                .build();
        try {
            dynamo.putItem(request);
            return notification;
        } catch (ConditionalCheckFailedException e) {
            return findByOrderIdAndType(notification.getOrderId(), notification.getType())
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public Optional<Notification> findByOrderIdAndType(String orderId, NotificationType type) {
        GetItemResponse response = dynamo.getItem(GetItemRequest.builder()
                .tableName(table)
                .key(Map.of("orderId", AttributeValue.fromS(orderId),
                            "type", AttributeValue.fromS(type.name())))
                .consistentRead(true)
                .build());
        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toDomain(response.item()));
    }

    @Override
    public List<Notification> findByOrderId(String orderId) {
        QueryRequest request = QueryRequest.builder()
                .tableName(table)
                .keyConditionExpression("orderId = :o")
                .expressionAttributeValues(Map.of(":o", AttributeValue.fromS(orderId)))
                .consistentRead(true)
                .build();
        return dynamo.queryPaginator(request).items().stream().map(this::toDomain).toList();
    }

    private Map<String, AttributeValue> toItem(Notification n) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("orderId", AttributeValue.fromS(n.getOrderId()));
        item.put("type", AttributeValue.fromS(n.getType().name()));
        item.put("id", AttributeValue.fromS(n.getId()));
        item.put("status", AttributeValue.fromS(n.getStatus().name()));
        item.put("createdAt", AttributeValue.fromS(n.getCreatedAt().toString()));
        if (n.getCustomerEmail() != null) {
            item.put("customerEmail", AttributeValue.fromS(n.getCustomerEmail()));
        }
        String error = n.getErrorMessage();
        if (error != null) {
            if (error.length() > MAX_ERROR_LENGTH) {
                error = error.substring(0, MAX_ERROR_LENGTH);
            }
            item.put("errorMessage", AttributeValue.fromS(error));
        }
        return item;
    }

    private Notification toDomain(Map<String, AttributeValue> item) {
        return Notification.reconstruct(
                item.get("id").s(),
                item.get("orderId").s(),
                NotificationType.valueOf(item.get("type").s()),
                item.containsKey("customerEmail") ? item.get("customerEmail").s() : null,
                NotificationStatus.valueOf(item.get("status").s()),
                Instant.parse(item.get("createdAt").s()),
                item.containsKey("errorMessage") ? item.get("errorMessage").s() : null);
    }
}