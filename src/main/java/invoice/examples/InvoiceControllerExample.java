package invoice.examples;

import invoice.services.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Example showing how to integrate notifications into your existing Invoice controller
 * This is just an example - integrate these calls into your actual InvoiceController
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceControllerExample {
    
    private final NotificationEventPublisher notificationEventPublisher;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createInvoice(
            @RequestBody Map<String, Object> invoiceData,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        // Your existing invoice creation logic here
        // ...
        
        // Generate or get invoice number
        String invoiceNumber = "INV-" + System.currentTimeMillis(); // Replace with actual invoice number generation
        
        // After successful invoice creation, publish notification event
        notificationEventPublisher.publishInvoiceCreatedEvent(userId, invoiceNumber);
        
        // Check if user is approaching invoice limit (implement your own logic)
        // if (userApproachingInvoiceLimit(userId)) {
        //     notificationEventPublisher.publishInvoiceLimitWarning(userId);
        // }
        
        return ResponseEntity.ok(Map.of("message", "Invoice created successfully", "invoiceNumber", invoiceNumber));
    }
    
    @PostMapping("/{id}/send")
    public ResponseEntity<Map<String, Object>> sendInvoice(
            @PathVariable Long id,
            @RequestBody Map<String, Object> sendData,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        // Your existing invoice sending logic here
        // ...
        
        // Get invoice and client details
        String invoiceNumber = "INV-001"; // Replace with actual invoice number retrieval
        String clientName = "Client Name"; // Replace with actual client name retrieval
        
        // After successful invoice sending, publish notification event
        notificationEventPublisher.publishInvoiceSentEvent(userId, clientName, invoiceNumber);
        
        return ResponseEntity.ok(Map.of("message", "Invoice sent successfully"));
    }
    
    // Example method to check invoice limits and send notifications
    @GetMapping("/check-limits")
    public ResponseEntity<Map<String, Object>> checkInvoiceLimits(Authentication authentication) {
        String userId = authentication.getName();
        
        // Your logic to check user's invoice limits
        // This is just an example - implement based on your business logic
        
        int currentInvoiceCount = 9; // Replace with actual count
        int maxInvoices = 10; // Replace with user's plan limit
        
        if (currentInvoiceCount >= maxInvoices) {
            notificationEventPublisher.publishInvoiceLimitReached(userId);
        } else if (currentInvoiceCount >= maxInvoices - 1) {
            notificationEventPublisher.publishInvoiceLimitWarning(userId);
        }
        
        return ResponseEntity.ok(Map.of("message", "Limits checked"));
    }
}