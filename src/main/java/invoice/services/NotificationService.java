package invoice.services;

import invoice.data.constants.NotificationType;
import invoice.data.models.User;
import invoice.dtos.response.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void createNotification(User user, String title, String message, NotificationType type, UUID relatedEntityId, String relatedEntityType);
    List<NotificationResponse> getUserNotifications(UUID userId);
    List<NotificationResponse> getUnreadNotifications(UUID userId);
    long getUnreadCount(UUID userId);
    void markAllAsRead(UUID userId);
    void markAsRead(UUID notificationId, UUID userId);
    void sendRealTimeNotification(User user, String title, String message, NotificationType type);
}