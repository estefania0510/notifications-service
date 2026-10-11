package co.andesexpress.notifications.config;

import co.andesexpress.notifications.domain.port.in.GetNotificationsInputPort;
import co.andesexpress.notifications.domain.port.in.ProcessEventInputPort;
import co.andesexpress.notifications.domain.port.out.NotificationRepositoryPort;
import co.andesexpress.notifications.domain.port.out.NotificationSenderPort;
import co.andesexpress.notifications.usecase.GetNotificationsUseCase;
import co.andesexpress.notifications.usecase.ProcessEventUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Los casos de uso no llevan anotaciones de Spring: se registran aquí como beans. */
@Configuration
public class UseCaseConfig {

    @Bean
    public ProcessEventInputPort processEventUseCase(NotificationRepositoryPort repository,
                                                     NotificationSenderPort sender) {
        return new ProcessEventUseCase(repository, sender);
    }

    @Bean
    public GetNotificationsInputPort getNotificationsUseCase(NotificationRepositoryPort repository) {
        return new GetNotificationsUseCase(repository);
    }
}