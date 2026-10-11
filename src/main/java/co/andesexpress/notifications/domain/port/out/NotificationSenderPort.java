package co.andesexpress.notifications.domain.port.out;

import co.andesexpress.notifications.domain.model.Notification;

/** Envía la notificación. Si falla, lanza una excepción. */
public interface NotificationSenderPort {
    void send(Notification notification);
}