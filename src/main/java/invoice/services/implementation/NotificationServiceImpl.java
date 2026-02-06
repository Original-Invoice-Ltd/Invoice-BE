package invoice.services.implementation;

import invoice.data.constants.NotificationType;
import invoice.data.models.Notification;
import invoice.data.models.NotificationsPreferences;
import invoice.data.models.User;
import invoice.data.repositories.NotificationRepository;
import invoice.data.repositories.UserRepository;
import invoice.dtos.response.NotificationResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.NotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
//    private final
    // Temporarily comment out Pusher to test compilation
    // private final Pusher pusher;
    
    @Override
    @Transactional
    public void createNotification(User user, String title, String message, NotificationType type, UUID relatedEntityId, String relatedEntityType) {
        try {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type);
            notification.setRelatedEntityId(relatedEntityId);
            notification.setRelatedEntityType(relatedEntityType);
            
            Notification saved = notificationRepository.save(notification);
            log.info("Created notification for user {}: {}", user.getEmail(), title);
            
            // Send real-time notification
            sendRealTimeNotification(user, title, message, type);
            
        } catch (Exception e) {
            log.error("Error creating notification for user {}: {}", user.getEmail(), e.getMessage());
        }
    }
    
    @Override
    public List<NotificationResponse> getUserNotifications(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        User user = userRepository.findUserById(userId).orElseThrow(()-> new OriginalInvoiceBaseException("Invalid user details provided"));
        NotificationsPreferences preferences = user.getSettings().getNotificationsPreferences();
        return notifications.stream()
        .filter(notification -> isNotificationEnabled(notification.getType(), preferences))
                .map(NotificationResponse::new)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        User user = userRepository.findUserById(userId).orElseThrow(()-> new OriginalInvoiceBaseException("Invalid user details provided"));
        NotificationsPreferences preferences = user.getSettings().getNotificationsPreferences();
        return notifications.stream()
                .filter(notification -> isNotificationEnabled(notification.getType(), preferences))
                .map(NotificationResponse::new)
                .collect(Collectors.toList());
    }
    
    @Override
    public long getUnreadCount(UUID userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new OriginalInvoiceBaseException("Invalid user details provided"));
        NotificationsPreferences preferences = user.getSettings().getNotificationsPreferences();
        return notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .filter(n -> isNotificationEnabled(n.getType(), preferences))
                .count();
    }
    
    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
//        notificationRepository.markAllAsReadByUserId(userId);
        User user = userRepository.findUserById(userId).orElseThrow(() -> new OriginalInvoiceBaseException("Invalid user details provided"));
        NotificationsPreferences preferences = user.getSettings().getNotificationsPreferences();
        List<Notification> enabledUnread = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .filter(n -> isNotificationEnabled(n.getType(), preferences))
                .toList();
        enabledUnread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(enabledUnread);
        log.info("Marked all enabled notifications as read for user: {}", userId);
    }

//    @Override
//    @Transactional
//    public void markAsRead(UUID notificationId, UUID userId) {
//        notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
//        log.info("Marked notification {} as read for user: {}", notificationId, userId);
//    }
@Override
@Transactional
public void markAsRead(UUID notificationId, UUID userId) {

    User user = userRepository.findUserById(userId).orElseThrow(() -> new OriginalInvoiceBaseException("Invalid user details provided"));
    NotificationsPreferences preferences = user.getSettings().getNotificationsPreferences();
    Notification notification = notificationRepository.findById(notificationId)
            .filter(n -> n.getUser().getId().equals(userId))
            .orElseThrow(() -> new OriginalInvoiceBaseException("Notification not found"));
    if (!isNotificationEnabled(notification.getType(), preferences))return;
    notification.setRead(true);
    notificationRepository.save(notification);
    log.info("Marked notification {} as read for user {}", notificationId, userId);
}


    @Override
    public void sendRealTimeNotification(User user, String title, String message, NotificationType type) {
        try {
            // Temporarily disable Pusher for testing
            log.info("Would send real-time notification to user {}: {} (Pusher disabled for testing)", user.getEmail(), title);
            
            /* TODO: Re-enable when Pusher is properly configured
            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("message", message);
            data.put("type", type.name());
            data.put("timestamp", System.currentTimeMillis());
            
            // Send to user-specific channel
            String channel = "user-" + user.getId().toString();
            pusher.trigger(channel, "notification", data);
            
            log.info("Sent real-time notification to user {}: {}", user.getEmail(), title);
            */
            
        } catch (Exception e) {
            log.error("Error sending real-time notification to user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private boolean isNotificationEnabled(NotificationType type, NotificationsPreferences preferences) {
        return switch (type) {

            case PAYMENT_RECEIVED,
                    PAYMENT_EVIDENCE_UPLOADED ->
                    preferences.isPaymentNotificationsEnabled();

            case INVOICE_CREATED,
                    INVOICE_UPDATED,
                    INVOICE_RECEIVED ->
                    preferences.isInvoiceNotificationsEnabled();

            case INVOICE_DELETED ->
                    preferences.isInvoiceReminderNotificationsEnabled();

            case CLIENT_CREATED,
                    CLIENT_UPDATED,
                    CLIENT_DELETED ->
                    preferences.isClientNotificationsEnabled();

            case SYSTEM_NOTIFICATION ->
                    preferences.isSystemNotificationsEnabled();
        };
    }
}