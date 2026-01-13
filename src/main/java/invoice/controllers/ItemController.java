package invoice.controllers;

import invoice.services.NotificationService;
import invoice.services.UserService;
import invoice.data.constants.NotificationType;
import invoice.data.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import invoice.data.repositories.UserRepository;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend-domain.com"})
public class ItemController {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createItem(
            Authentication authentication,
            @RequestBody Map<String, Object> itemData) {
        
        try {
            User currentUser = getCurrentUser();
            
            // Your existing item creation logic here
            // ...
            
            // Example item creation
            String itemName = (String) itemData.get("name");
            String itemId = "item_" + System.currentTimeMillis(); // Replace with actual ID
            
            // Create notification
            notificationService.createNotification(
                currentUser,
                "New item created",
                "\"" + itemName + "\" has been added to your items.",
                NotificationType.SYSTEM_NOTIFICATION,
                UUID.fromString(itemId.replace("item_", "")),
                "ITEM"
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Item created successfully",
                "itemId", itemId,
                "itemName", itemName
            ));
        } catch (Exception e) {
            log.error("Error creating item: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteItem(
            Authentication authentication,
            @PathVariable String itemId,
            @RequestParam String itemName) {
        
        try {
            User currentUser = getCurrentUser();
            
            // Your existing item deletion logic here
            // ...
            
            // Create notification
            notificationService.createNotification(
                currentUser,
                "Item deleted",
                "\"" + itemName + "\" has been removed from your list.",
                NotificationType.SYSTEM_NOTIFICATION,
                UUID.fromString(itemId.replace("item_", "")),
                "ITEM"
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Item deleted successfully",
                "itemId", itemId
            ));
        } catch (Exception e) {
            log.error("Error deleting item: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getItems(Authentication authentication) {
        // Your existing logic to fetch items
        return ResponseEntity.ok(Map.of("items", "Your item list here"));
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}