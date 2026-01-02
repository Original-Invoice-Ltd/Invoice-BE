package invoice.examples;

import invoice.services.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Example showing how to integrate system notifications into your existing controllers
 * This is just an example - integrate these calls into your actual controllers
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemNotificationExample {
    
    private final NotificationEventPublisher notificationEventPublisher;
    
    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, Object> profileData,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        // Your existing profile update logic here
        // ...
        
        // After successful profile update, publish notification event
        notificationEventPublisher.publishProfileUpdated(userId);
        
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }
    
    @PutMapping("/password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, Object> passwordData,
            Authentication authentication) {
        
        String userId = authentication.getName();
        
        // Your existing password change logic here
        // ...
        
        // After successful password change, publish notification event
        notificationEventPublisher.publishPasswordChanged(userId);
        
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    // Example endpoint for admin to send feature announcements
    @PostMapping("/announce-feature")
    public ResponseEntity<Map<String, Object>> announceNewFeature(
            @RequestBody Map<String, Object> featureData,
            Authentication authentication) {
        
        String featureName = (String) featureData.get("featureName");
        String description = (String) featureData.get("description");
        
        // In a real scenario, you'd send this to all users or specific user groups
        String userId = authentication.getName(); // For demo, just send to current user
        
        notificationEventPublisher.publishNewFeatureAlert(userId, featureName, description);
        
        return ResponseEntity.ok(Map.of("message", "Feature announcement sent"));
    }
    
    // Example method to be called during login process
    @PostMapping("/login-check")
    public ResponseEntity<Map<String, Object>> checkLoginDevice(
            @RequestBody Map<String, Object> loginData,
            Authentication authentication) {
        
        String userId = authentication.getName();
        boolean isNewDevice = (Boolean) loginData.getOrDefault("isNewDevice", false);
        
        if (isNewDevice) {
            notificationEventPublisher.publishLoginFromNewDevice(userId);
        }
        
        return ResponseEntity.ok(Map.of("message", "Login check completed"));
    }
}