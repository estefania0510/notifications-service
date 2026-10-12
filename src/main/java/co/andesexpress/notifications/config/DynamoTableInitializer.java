package co.andesexpress.notifications.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Crea la tabla de notificaciones al arrancar si no existe en la cuenta
 * de las credenciales activas. Si ya existe, no hace nada.
 */
@Component
public class DynamoTableInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DynamoTableInitializer.class);

    private final DynamoDbClient dynamo;
    private final String tableName;

    public DynamoTableInitializer(DynamoDbClient dynamo,
                                  @Value("${app.dynamodb.table-name:notifications}") String tableName) {
        this.dynamo = dynamo;
        this.tableName = tableName;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            dynamo.describeTable(b -> b.tableName(tableName));
            log.info("Tabla DynamoDB '{}' ya existe", tableName);
        } catch (ResourceNotFoundException e) {
            log.info("Tabla DynamoDB '{}' no existe, creándola...", tableName);
            dynamo.createTable(b -> b
                    .tableName(tableName)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("orderId").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder()
                                    .attributeName("type").attributeType(ScalarAttributeType.S).build())
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("orderId").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder()
                                    .attributeName("type").keyType(KeyType.RANGE).build()));
            dynamo.waiter().waitUntilTableExists(b -> b.tableName(tableName));
            log.info("Tabla DynamoDB '{}' creada y activa", tableName);
        }
    }
}