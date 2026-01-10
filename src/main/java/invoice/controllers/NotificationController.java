package invoice.controllers;

import invoice.dtos.response.NotificationResponse;
import invoice.services.NotificationService;
import invoice.data.models.User;
import invoice.data.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@AllArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    
    @GetMapping("/all")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        try {
            User currentUser = getCurrentUser();
            List<NotificationResponse> notifications = notificationService.getUserNotifications(currentUser.getId());
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            log.error("Error fetching notifications: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications() {
        try {
            User currentUser = getCurrentUser();
            List<NotificationResponse> notifications = notificationService.getUnreadNotifications(currentUser.getId());
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            log.error("Error fetching unread notifications: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        try {
            User currentUser = getCurrentUser();
            long count = notificationService.getUnreadCount(currentUser.getId());
            return ResponseEntity.ok(count);
            
        } catch (Exception e) {
            log.error("Error fetching unread count: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/mark-all-read")
    public ResponseEntity<String> markAllAsRead() {
        try {
            User currentUser = getCurrentUser();
            notificationService.markAllAsRead(currentUser.getId());
            return ResponseEntity.ok("All notifications marked as read");
            
        } catch (Exception e) {
            log.error("Error marking all notifications as read: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{notificationId}/mark-read")
    public ResponseEntity<String> markAsRead(@PathVariable UUID notificationId) {
        try {
            User currentUser = getCurrentUser();
            notificationService.markAsRead(notificationId, currentUser.getId());
            return ResponseEntity.ok("Notification marked as read");
            
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}