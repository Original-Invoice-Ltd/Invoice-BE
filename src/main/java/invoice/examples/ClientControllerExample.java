package invoice.examples;

import invoice.services.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Example showing how to integrate notifications into your existing Client controller
 * This is just an example - integrate these calls into your actual ClientController
 */
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientControllerExample {
    
    private final NotificationEventPublisher notificationEventPublisher;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createClient(
            @RequestBody Map<String, Object> clientData,
            Authentication authentication) {
        
        String userId = authentication.getName();
        String clientName = (String) clientData.get("name");
        
        // Your existing client creation logic here
        // ...
        
        // After successful client creation, publish notification event
        notificationEventPublisher.publishClientAddedEvent(userId, clientName);
        
        return ResponseEntity.ok(Map.of("message", "Client created successfully"));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateClient(
            @PathVariable Long id,
            @RequestBody Map<String, Object> clientData,
            Authentication authentication) {
        
        String userId = authentication.getName();
        String clientName = (String) clientData.get("name");
        
        // Your existing client update logic here
        // ...
        
        // After successful client update, publish notification event
        notificationEventPublisher.publishClientUpdatedEvent(userId, clientName);
        
        return ResponseEntity.ok(Map.of("message", "Client updated successfully"));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteClient(
            @PathVariable Long id,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        // Get client name before deletion for notification
        // String clientName = clientService.getClientName(id);
        String clientName = "Example Client"; // Replace with actual client name retrieval
        
        // Your existing client deletion logic here
        // ...
        
        // After successful client deletion, publish notification event
        notificationEventPublisher.publishClientDeletedEvent(userId, clientName);
        
        return ResponseEntity.ok(Map.of("message", "Client deleted successfully"));
    }
}