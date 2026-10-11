package co.andesexpress.notifications.domain.model;

public enum NotificationType {
    ORDER_CONFIRMED,
    GUIDE_READY;

    /** Traduce el nombre del evento que llega por la cola al tipo de notificación. */
    public static NotificationType fromEventType(String eventType) {
        if (eventType == null) {
            throw new IllegalArgumentException("El tipo de evento es obligatorio");
        }
        return switch (eventType) {
            case "PedidoConfirmado" -> ORDER_CONFIRMED;
            case "GuiaLista" -> GUIDE_READY;
            default -> throw new IllegalArgumentException("Tipo de evento desconocido: " + eventType);
        };
    }

}
