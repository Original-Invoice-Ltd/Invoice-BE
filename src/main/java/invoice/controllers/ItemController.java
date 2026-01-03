package invoice.controllers;

import invoice.services.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend-domain.com"})
public class ItemController {
    
    private final NotificationEventPublisher notificationEventPublisher;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createItem(
            Authentication authentication,
            @RequestBody Map<String, Object> itemData) {
        
        String userId = authentication.getName();
        
        // Your existing item creation logic here
        // ...
        
        // Example item creation
        String itemName = (String) itemData.get("name");
        String itemId = "item_" + System.currentTimeMillis(); // Replace with actual ID
        
        // Publish notification event
        notificationEventPublisher.publishItemAddedEvent(userId, itemName, itemId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Item created successfully",
            "itemId", itemId,
            "itemName", itemName
        ));
    }
    
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteItem(
            Authentication authentication,
            @PathVariable String itemId,
            @RequestParam String itemName) {
        
        String userId = authentication.getName();
        
        // Your existing item deletion logic here
        // ...
        
        // Publish notification event
        notificationEventPublisher.publishItemDeletedEvent(userId, itemName, itemId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Item deleted successfully",
            "itemId", itemId
        ));
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getItems(Authentication authentication) {
        // Your existing logic to fetch items
        return ResponseEntity.ok(Map.of("items", "Your item list here"));
    }
}