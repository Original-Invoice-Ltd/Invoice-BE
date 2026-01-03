package invoice.events;

import invoice.entities.Notification;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NotificationEvent extends ApplicationEvent {
    
    private final String userId;
    private final Notification.NotificationType type;
    private final String title;
    private final String message;
    private final String relatedEntityId;
    private final String relatedEntityType;
    
    public NotificationEvent(Object source, String userId, Notification.NotificationType type, 
                           String title, String message, String relatedEntityId, String relatedEntityType) {
        super(source);
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
    }
    
    public NotificationEvent(Object source, String userId, Notification.NotificationType type, 
                           String title, String message) {
        this(source, userId, type, title, message, null, null);
    }
}