package invoice.dto;

import invoice.entities.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    
    private Long id;
    private Notification.NotificationType type;
    private String title;
    private String message;
    private Boolean isRead;
    private Boolean isNew;
    private String timestamp;
    private String relatedEntityId;
    private String relatedEntityType;
    
    public static NotificationDTO fromEntity(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setIsRead(notification.getIsRead());
        dto.setIsNew(notification.getIsNew());
        dto.setTimestamp(formatTimestamp(notification.getCreatedAt()));
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRelatedEntityType(notification.getRelatedEntityType());
        return dto;
    }
    
    private static String formatTimestamp(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);
        
        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " mins ago";
        } else if (hours < 24) {
            return hours + " hours ago";
        } else if (days == 1) {
            return "Yesterday";
        } else if (days < 7) {
            return days + " days ago";
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
    }
}