package invoice.examples;

import invoice.services.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Example showing how to integrate notifications into your existing Item/Product controller
 * This is just an example - integrate these calls into your actual ItemController
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemControllerExample {
    
    private final NotificationEventPublisher notificationEventPublisher;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createItem(
            @RequestBody Map<String, Object> itemData,
            Authentication authentication) {
        
        String userId = authentication.getName();
        String itemName = (String) itemData.get("name");
        
        // Your existing item creation logic here
        // ...
        
        // After successful item creation, publish notification event
        notificationEventPublisher.publishItemAddedEvent(userId, itemName);
        
        return ResponseEntity.ok(Map.of("message", "Item created successfully"));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateItem(
            @PathVariable Long id,
            @RequestBody Map<String, Object> itemData,
            Authentication authentication) {
        
        String userId = authentication.getName();
        String itemName = (String) itemData.get("name");
        
        // Your existing item update logic here
        // ...
        
        // After successful item update, publish notification event
        notificationEventPublisher.publishItemUpdatedEvent(userId, itemName);
        
        return ResponseEntity.ok(Map.of("message", "Item updated successfully"));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteItem(
            @PathVariable Long id,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        // Get item name before deletion for notification
        // String itemName = itemService.getItemName(id);
        String itemName = "Example Item"; // Replace with actual item name retrieval
        
        // Your existing item deletion logic here
        // ...
        
        // After successful item deletion, publish notification event
        notificationEventPublisher.publishItemDeletedEvent(userId, itemName);
        
        return ResponseEntity.ok(Map.of("message", "Item deleted successfully"));
    }
}