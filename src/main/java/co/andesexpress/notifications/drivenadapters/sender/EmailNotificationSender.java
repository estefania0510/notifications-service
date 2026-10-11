package co.andesexpress.notifications.drivenadapters.sender;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.model.NotificationType;
import co.andesexpress.notifications.domain.port.out.NotificationSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envío simulado por correo: manda un email al SMTP configurado (Mailpit en local). */
@Component
@ConditionalOnProperty(name = "notifications.channel", havingValue = "email")
public class EmailNotificationSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailNotificationSender(JavaMailSender mailSender,
                                   @Value("${notifications.email.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(Notification notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notification.getCustomerEmail());
        message.setSubject(subjectFor(notification.getType()));
        message.setText(bodyFor(notification));

        mailSender.send(message); // si falla lanza una excepción y la notificación queda FAILED
        log.info("Correo enviado: tipo={} pedido={} para={}",
                notification.getType(), notification.getOrderId(), notification.getCustomerEmail());
    }

    private String subjectFor(NotificationType type) {
        return switch (type) {
            case ORDER_CONFIRMED -> "Tu pedido fue confirmado";
            case GUIDE_READY -> "La guía de tu pedido está lista";
        };
    }

    private String bodyFor(Notification n) {
        return switch (n.getType()) {
            case ORDER_CONFIRMED -> "Hola, tu pedido " + n.getOrderId()
                    + " fue confirmado. Te avisaremos cuando la guía de envío esté lista.\n\nAndesExpress";
            case GUIDE_READY -> "Hola, la guía de envío de tu pedido " + n.getOrderId()
                    + " ya está lista.\n\nAndesExpress";
        };
    }
}