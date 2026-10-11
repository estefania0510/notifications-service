package co.andesexpress.notifications.entrypoints.web.sqs;

import co.andesexpress.notifications.domain.port.in.ProcessEventInputPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Lee los eventos que publica orders y se los pasa al caso de uso. */
@Component
public class OrderEventsSqsConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsSqsConsumer.class);

    /** Forma del mensaje que publica orders: {"eventType": "...", "orderId": "..."} */
    public record EventMessage(String eventType, String orderId) {
    }

    private final SqsClient sqsClient;
    private final ProcessEventInputPort processEvent;
    private final JsonMapper jsonMapper;
    private final String queueUrl;

    public OrderEventsSqsConsumer(SqsClient sqsClient,
                                  ProcessEventInputPort processEvent,
                                  JsonMapper jsonMapper,
                                  @Value("${aws.sqs.order-confirmed-queue-url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.processEvent = processEvent;
        this.jsonMapper = jsonMapper;
        this.queueUrl = queueUrl;
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        List<Message> messages;
        try {
            messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(5)
                    .build()).messages();
        } catch (RuntimeException e) {
            log.warn("No se pudo leer la cola: {}", e.getMessage());
            return;
        }
        messages.forEach(this::handle);
    }

    private void handle(Message message) {
        EventMessage event;
        try {
            event = jsonMapper.readValue(message.body(), EventMessage.class);
        } catch (RuntimeException e) {
            log.warn("Mensaje con formato inválido, se descarta: {}", message.body());
            delete(message);
            return;
        }

        try {
            processEvent.processEvent(event.eventType(), event.orderId());
            delete(message);
        } catch (IllegalArgumentException e) {
            // Reintentar no arregla un evento inválido: se descarta.
            log.warn("Evento inválido, se descarta: {}", e.getMessage());
            delete(message);
        } catch (RuntimeException e) {
            // Fallo temporal: no se borra, SQS lo entregará de nuevo.
            log.error("Falló el procesamiento, el mensaje volverá a la cola: {}", e.getMessage());
        }
    }

    private void delete(Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }
}