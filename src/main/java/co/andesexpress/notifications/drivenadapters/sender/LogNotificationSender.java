package co.andesexpress.notifications.drivenadapters.sender;

import co.andesexpress.notifications.domain.model.Notification;
import co.andesexpress.notifications.domain.port.out.NotificationSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Envío simulado: solo escribe un log. */
@Component
public class LogNotificationSender implements NotificationSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(Notification notification) {
        log.info("[NOTIFICACION SIMULADA] tipo={} pedido={} id={}",
                notification.getType(), notification.getOrderId(), notification.getId());
    }
}