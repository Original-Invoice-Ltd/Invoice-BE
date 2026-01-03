package invoice.services;

import invoice.entities.Notification;
import invoice.events.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public void publishClientAddedEvent(String userId, String clientName, String clientId) {
        String title = "New client added";
        String message = String.format("%s has been added to your client list and is ready to invoice.", clientName);
        
        NotificationEvent event = new NotificationEvent(
            this, userId, Notification.NotificationType.CLIENT, 
            title, message, clientId, "CLIENT"
        );
        eventPublisher.publishEvent(event);
    }
    
    public void publishItemAddedEvent(String userId, String itemName, String itemId) {
        String title = "New item created";
        String message = String.format("\"%s\" has been added to your items.", itemName);
        
        NotificationEvent event = new NotificationEvent(
            this, userId, Notification.NotificationType.ITEM, 
            title, message, itemId, "ITEM"
        );
        eventPublisher.publishEvent(event);
    }
    
    public void publishItemDeletedEvent(String userId, String itemName, String itemId) {
        String title = "Item deleted";
        String message = String.format("\"%s\" has been removed from your list.", itemName);
        
        NotificationEvent event = new NotificationEvent(
            this, userId, Notification.NotificationType.ITEM, 
            title, message, itemId, "ITEM"
        );
        eventPublisher.publishEvent(event);
    }
    
    public void publishInvoiceSentEvent(String userId, String clientName, String invoiceId) {
        String title = "Invoice sent";
        String message = String.format("Your invoice to %s was delivered successfully and is now viewable.", clientName);
        
        NotificationEvent event = new NotificationEvent(
            this, userId, Notification.NotificationType.INVOICE, 
            title, message, invoiceId, "INVOICE"
        );
        eventPublisher.publishEvent(event);
    }
    
    public void publishInvoiceLimitReachedEvent(String userId) {
        String title = "Invoice limit reached";
        String message = "Upgrade your plan to create more invoices.";
        
        NotificationEvent event = new NotificationEvent(
            this, userId, Notification.NotificationType.SYSTEM, 
            title, message, null, "SYSTEM"
        );
        eventPublisher.publishEvent(event);
    }
    
    public void publishPaymentReceivedEvent(String userId, String amount, String clientName, String invoiceId) {
        String title = "Payment received";
        String message = String.format("Payment of %s received from %s.", amount, clientName);
        
        NotificationEvent event = new NotificationEvent(
            this, userId, Notification.NotificationType.PAYMENT, 
            title, message, invoiceId, "PAYMENT"
        );
        eventPublisher.publishEvent(event);
    }
    
    public void publishSystemMaintenanceEvent(String userId, String maintenanceDetails) {
        String title = "System maintenance scheduled";
        String message = maintenanceDetails;
        
        NotificationEvent event = new NotificationEvent(
            this, userId, Notification.NotificationType.SYSTEM, 
            title, message, null, "SYSTEM"
        );
        eventPublisher.publishEvent(event);
    }
}