package co.andesexpress.notifications.drivenadapters.sender;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.model.NotificationType;
import co.andesexpress.notifications.domain.port.out.NotificationSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Envío simulado por webhook: hace un POST con un JSON a la URL configurada. */
@Component
@ConditionalOnProperty(name = "notifications.channel", havingValue = "webhook")
public class WebhookNotificationSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationSender.class);

    private final JsonMapper jsonMapper;
    private final String webhookUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public WebhookNotificationSender(JsonMapper jsonMapper,
                                     @Value("${notifications.webhook.url}") String webhookUrl) {
        this.jsonMapper = jsonMapper;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void send(Notification notification) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notificationId", notification.getId());
        payload.put("orderId", notification.getOrderId());
        payload.put("type", notification.getType().name());
        payload.put("customerEmail", notification.getCustomerEmail());
        payload.put("message", messageFor(notification.getType()));
        payload.put("createdAt", notification.getCreatedAt().toString());

        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(payload)))
                .build();

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("El webhook respondió " + response.statusCode());
            }
            log.info("Webhook enviado: tipo={} pedido={} status={}",
                    notification.getType(), notification.getOrderId(), response.statusCode());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo llamar al webhook: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envío al webhook interrumpido", e);
        }
    }

    private String messageFor(NotificationType type) {
        return switch (type) {
            case ORDER_CONFIRMED -> "Tu pedido fue confirmado";
            case GUIDE_READY -> "La guía de envío de tu pedido está lista";
        };
    }
}