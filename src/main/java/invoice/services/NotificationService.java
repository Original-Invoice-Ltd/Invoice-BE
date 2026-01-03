package invoice.services;

import invoice.dto.NotificationDTO;
import invoice.entities.Notification;
import invoice.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    public void createNotification(String userId, Notification.NotificationType type, 
                                 String title, String message, String relatedEntityId, 
                                 String relatedEntityType) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setRelatedEntityId(relatedEntityId);
            notification.setRelatedEntityType(relatedEntityType);
            
            notificationRepository.save(notification);
            log.info("Notification created for user: {} with type: {}", userId, type);
        } catch (Exception e) {
            log.error("Error creating notification for user: {}", userId, e);
        }
    }
    
    public Page<NotificationDTO> getUserNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return notifications.map(NotificationDTO::fromEntity);
    }
    
    public Page<NotificationDTO> getUserNotificationsByType(String userId, Notification.NotificationType type, 
                                                          int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
        return notifications.map(NotificationDTO::fromEntity);
    }
    
    public List<NotificationDTO> getUnreadNotifications(String userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return notifications.stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("All notifications marked as read for user: {}", userId);
    }
    
    @Transactional
    public void markAllAsNotNew(String userId) {
        notificationRepository.markAllAsNotNewByUserId(userId);
    }
    
    @Transactional
    public void markAsRead(Long notificationId, String userId) {
        notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
    }
    
    // Specific notification creation methods
    public void createClientAddedNotification(String userId, String clientName, String clientId) {
        String title = "New client added";
        String message = String.format("%s has been added to your client list and is ready to invoice.", clientName);
        createNotification(userId, Notification.NotificationType.CLIENT, title, message, clientId, "CLIENT");
    }
    
    public void createItemAddedNotification(String userId, String itemName, String itemId) {
        String title = "New item created";
        String message = String.format("\"%s\" has been added to your items.", itemName);
        createNotification(userId, Notification.NotificationType.ITEM, title, message, itemId, "ITEM");
    }
    
    public void createItemDeletedNotification(String userId, String itemName, String itemId) {
        String title = "Item deleted";
        String message = String.format("\"%s\" has been removed from your list.", itemName);
        createNotification(userId, Notification.NotificationType.ITEM, title, message, itemId, "ITEM");
    }
    
    public void createInvoiceSentNotification(String userId, String clientName, String invoiceId) {
        String title = "Invoice sent";
        String message = String.format("Your invoice to %s was delivered successfully and is now viewable.", clientName);
        createNotification(userId, Notification.NotificationType.INVOICE, title, message, invoiceId, "INVOICE");
    }
    
    public void createInvoiceLimitReachedNotification(String userId) {
        String title = "Invoice limit reached";
        String message = "Upgrade your plan to create more invoices.";
        createNotification(userId, Notification.NotificationType.SYSTEM, title, message, null, "SYSTEM");
    }
    
    public void createPaymentReceivedNotification(String userId, String amount, String clientName, String invoiceId) {
        String title = "Payment received";
        String message = String.format("Payment of %s received from %s.", amount, clientName);
        createNotification(userId, Notification.NotificationType.PAYMENT, title, message, invoiceId, "PAYMENT");
    }
    
    public void createSystemMaintenanceNotification(String userId, String maintenanceDetails) {
        String title = "System maintenance scheduled";
        String message = maintenanceDetails;
        createNotification(userId, Notification.NotificationType.SYSTEM, title, message, null, "SYSTEM");
    }
}