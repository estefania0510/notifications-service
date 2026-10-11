package co.andesexpress.notifications.entrypoints.web;

import co.andesexpress.notifications.domain.port.in.GetNotificationsInputPort;
import co.andesexpress.notifications.domain.port.in.ProcessEventInputPort;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final ProcessEventInputPort processEvent;
    private final GetNotificationsInputPort getNotifications;

    public NotificationController(ProcessEventInputPort processEvent,
                                  GetNotificationsInputPort getNotifications) {
        this.processEvent = processEvent;
        this.getNotifications = getNotifications;
    }

    @PostMapping("/events")
    public NotificationResponse receiveEvent(@Valid @RequestBody EventRequest request) {
        return NotificationResponse.from(
                processEvent.processEvent(request.eventType(), request.orderId(), request.customerEmail()));
    }

    @GetMapping("/order/{orderId}")
    public List<NotificationResponse> getByOrder(@PathVariable String orderId) {
        return getNotifications.getByOrderId(orderId).stream()
                .map(NotificationResponse::from)
                .toList();
    }
}