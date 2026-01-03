package invoice.events;

import invoice.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {
    
    private final NotificationService notificationService;
    
    @EventListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            notificationService.createNotification(
                event.getUserId(),
                event.getType(),
                event.getTitle(),
                event.getMessage(),
                event.getRelatedEntityId(),
                event.getRelatedEntityType()
            );
            log.info("Notification event processed for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error processing notification event for user: {}", event.getUserId(), e);
        }
    }
}