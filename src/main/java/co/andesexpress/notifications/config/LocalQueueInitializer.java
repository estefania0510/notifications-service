package co.andesexpress.notifications.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

/** Solo para el emulador local: crea la cola al arrancar si no existe. */
@Component
public class LocalQueueInitializer {

    private static final Logger log = LoggerFactory.getLogger(LocalQueueInitializer.class);

    private final SqsClient sqsClient;
    private final String endpoint;
    private final String queueUrl;

    public LocalQueueInitializer(SqsClient sqsClient,
                                 @Value("${aws.sqs.endpoint:}") String endpoint,
                                 @Value("${aws.sqs.order-confirmed-queue-url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.endpoint = endpoint;
        this.queueUrl = queueUrl;
    }

    @PostConstruct
    public void createQueueIfLocal() {
        if (endpoint.isBlank()) {
            return; // AWS real: la cola se crea en la consola de AWS
        }
        String name = queueUrl.substring(queueUrl.lastIndexOf('/') + 1);
        try {
            String url = sqsClient.createQueue(CreateQueueRequest.builder().queueName(name).build()).queueUrl();
            log.info("Cola local lista: {}", url);
        } catch (RuntimeException e) {
            log.warn("No se pudo crear la cola local: {}", e.getMessage());
        }
    }
}