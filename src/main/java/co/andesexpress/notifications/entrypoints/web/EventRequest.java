package co.andesexpress.notifications.entrypoints.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EventRequest(
        @NotBlank(message = "eventType es obligatorio") String eventType,
        @NotBlank(message = "orderId es obligatorio") String orderId,
        @Email(message = "customerEmail no es válido") String customerEmail) {
}