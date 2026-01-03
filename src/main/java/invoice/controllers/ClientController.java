package invoice.controllers;

import invoice.services.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "https://your-frontend-domain.com"})
public class ClientController {
    
    private final NotificationEventPublisher notificationEventPublisher;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createClient(
            Authentication authentication,
            @RequestBody Map<String, Object> clientData) {
        
        String userId = authentication.getName();
        
        // Your existing client creation logic here
        // ...
        
        // Example client creation
        String clientName = (String) clientData.get("name");
        String clientId = "client_" + System.currentTimeMillis(); // Replace with actual ID
        
        // Publish notification event
        notificationEventPublisher.publishClientAddedEvent(userId, clientName, clientId);
        
        return ResponseEntity.ok(Map.of(
            "message", "Client created successfully",
            "clientId", clientId,
            "clientName", clientName
        ));
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getClients(Authentication authentication) {
        // Your existing logic to fetch clients
        return ResponseEntity.ok(Map.of("clients", "Your client list here"));
    }
}