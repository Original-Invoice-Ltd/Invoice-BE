package invoice.controllers;

import invoice.services.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend-domain.com"})
public class InvoiceController {
    
    private final NotificationEventPublisher notificationEventPublisher;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createInvoice(
            Authentication authentication,
            @RequestBody Map<String, Object> invoiceData) {
        
        String userId = authentication.getName();
        
        // Your existing invoice creation logic here
        // Check if user has reached invoice limit
        // ...
        
        String invoiceId = "invoice_" + System.currentTimeMillis(); // Replace with actual ID
        
        return ResponseEntity.ok(Map.of(
            "message", "Invoice created successfully",
            "invoiceId", invoiceId
        ));
    }
    
    @PostMapping("/{invoiceId}/send")
    public ResponseEntity<Map<String, Object>> sendInvoice(
            Authentication authentication,
            @PathVariable String invoiceId,
            @RequestBody Map<String, Object> sendData) {
        
        String userId = authentication.getName();
        String clientName = (String) sendData.get("clientName");
        
        // Your existing invoice sending logic here
        // ...
        
        // Publish notification event
        notificationEventPublisher.publishInvoiceSentEvent(userId, clientName, invoiceId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Invoice sent successfully",
            "invoiceId", invoiceId
        ));
    }
    
    @PostMapping("/limit-reached")
    public ResponseEntity<Map<String, Object>> handleInvoiceLimitReached(Authentication authentication) {
        String userId = authentication.getName();
        
        // Publish notification event
        notificationEventPublisher.publishInvoiceLimitReachedEvent(userId);
        
        return ResponseEntity.ok(Map.of("message", "Invoice limit notification sent"));
    }
    
    @PostMapping("/{invoiceId}/payment-received")
    public ResponseEntity<Map<String, Object>> recordPayment(
            Authentication authentication,
            @PathVariable String invoiceId,
            @RequestBody Map<String, Object> paymentData) {
        
        String userId = authentication.getName();
        String amount = (String) paymentData.get("amount");
        String clientName = (String) paymentData.get("clientName");
        
        // Your existing payment recording logic here
        // ...
        
        // Publish notification event
        notificationEventPublisher.publishPaymentReceivedEvent(userId, amount, clientName, invoiceId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Payment recorded successfully",
            "invoiceId", invoiceId
        ));
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getInvoices(Authentication authentication) {
        // Your existing logic to fetch invoices
        return ResponseEntity.ok(Map.of("invoices", "Your invoice list here"));
    }
}