package invoice.controllers;

import invoice.data.models.Subscription;
import invoice.data.models.User;
import invoice.services.PaystackSubscriptionService;
import invoice.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://www.originalinvoice.com"}, allowCredentials = "true")
public class SubscriptionController {
    
    private final PaystackSubscriptionService subscriptionService;
    private final UserService userService;
    
    /**
     * Get current user's subscription details
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSubscription(Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            Optional<Subscription> subscriptionOpt = subscriptionService.getUserSubscription(user);
            
            if (subscriptionOpt.isEmpty()) {
                // Return free plan details
                Map<String, Object> freePlan = new HashMap<>();
                freePlan.put("plan", "FREE");
                freePlan.put("status", "ACTIVE");
                freePlan.put("invoiceLimit", 3);
                freePlan.put("invoicesUsed", 0);
                freePlan.put("sharingEnabled", false);
                freePlan.put("logoLimit", 0);
                freePlan.put("logosUploaded", 0);
                return ResponseEntity.ok(freePlan);
            }
            
            Subscription subscription = subscriptionOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", subscription.getId());
            response.put("plan", subscription.getPlan().name());
            response.put("planDisplayName", subscription.getPlan().getDisplayName());
            response.put("status", subscription.getStatus().name());
            response.put("invoiceLimit", subscription.getPlan().getInvoiceLimit());
            response.put("invoicesUsed", subscription.getInvoicesUsedThisMonth());
            response.put("sharingEnabled", subscription.getPlan().isSharingEnabled());
            response.put("logoLimit", subscription.getPlan().getLogoLimit());
            response.put("logosUploaded", subscription.getLogosUploadedThisMonth());
            response.put("currentPeriodStart", subscription.getCurrentPeriodStart());
            response.put("currentPeriodEnd", subscription.getCurrentPeriodEnd());
            response.put("nextPaymentDate", subscription.getNextPaymentDate());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch subscription"));
        }
    }
    
    /**
     * Initialize a new subscription
     */
    @PostMapping("/initialize")
    public ResponseEntity<?> initializeSubscription(
        @RequestBody Map<String, String> request,
        Principal principal
    ) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            String planName = request.get("plan");
            Subscription.SubscriptionPlan plan;
            
            try {
                plan = Subscription.SubscriptionPlan.valueOf(planName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid plan name"));
            }
            
            if (plan == Subscription.SubscriptionPlan.FREE) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot subscribe to free plan"));
            }
            
            Map<String, Object> result = subscriptionService.initializeSubscription(user, plan);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error initializing subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to initialize subscription"));
        }
    }
    
    /**
     * Cancel current subscription
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            Map<String, Object> result = subscriptionService.cancelSubscription(user);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error cancelling subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to cancel subscription"));
        }
    }
    
    /**
     * Check if user can create an invoice
     */
    @GetMapping("/can-create-invoice")
    public ResponseEntity<?> canCreateInvoice(Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            boolean canCreate = subscriptionService.canCreateInvoice(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("canCreate", canCreate);
            
            if (!canCreate) {
                Optional<Subscription> subscriptionOpt = subscriptionService.getUserSubscription(user);
                subscriptionOpt.ifPresent(subscription -> {
                    response.put("reason", "Invoice limit reached for your plan");
                    response.put("limit", subscription.getPlan().getInvoiceLimit());
                    response.put("used", subscription.getInvoicesUsedThisMonth());
                });
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking invoice creation permission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check permission"));
        }
    }
    
    /**
     * Get available plans
     */
    @GetMapping("/plans")
    public ResponseEntity<?> getPlans() {
        try {
            Map<String, Object> plans = new HashMap<>();
            
            for (Subscription.SubscriptionPlan plan : Subscription.SubscriptionPlan.values()) {
                Map<String, Object> planDetails = new HashMap<>();
                planDetails.put("name", plan.name());
                planDetails.put("displayName", plan.getDisplayName());
                planDetails.put("amount", plan.getAmountInKobo() / 100.0); // Convert to Naira
                planDetails.put("invoiceLimit", plan.getInvoiceLimit());
                planDetails.put("sharingEnabled", plan.isSharingEnabled());
                planDetails.put("logoLimit", plan.getLogoLimit());
                planDetails.put("unlimitedInvoices", plan.isUnlimitedInvoices());
                planDetails.put("unlimitedLogos", plan.isUnlimitedLogos());
                
                plans.put(plan.name(), planDetails);
            }
            
            return ResponseEntity.ok(plans);
            
        } catch (Exception e) {
            log.error("Error fetching plans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch plans"));
        }
    }
    
    /**
     * Webhook endpoint for Paystack events
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Received webhook: {}", payload);
            subscriptionService.handleWebhookEvent(payload);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Error handling webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process webhook"));
        }
    }
    
    /**
     * Initialize transaction with plan (alternative subscription flow)
     */
    @PostMapping("/initialize-transaction")
    public ResponseEntity<?> initializeTransactionWithPlan(
        @RequestBody Map<String, Object> request,
        Principal principal
    ) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            String planName = (String) request.get("plan");
            Subscription.SubscriptionPlan plan;
            
            try {
                plan = Subscription.SubscriptionPlan.valueOf(planName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid plan name"));
            }
            
            if (plan == Subscription.SubscriptionPlan.FREE) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot subscribe to free plan"));
            }
            
            // Extract payment channels if provided
            String[] channels = null;
            if (request.containsKey("channels")) {
                Object channelsObj = request.get("channels");
                if (channelsObj instanceof String[]) {
                    channels = (String[]) channelsObj;
                } else if (channelsObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> channelsList = (java.util.List<String>) channelsObj;
                    channels = channelsList.toArray(new String[0]);
                }
            }
            
            // Extract callback URL if provided
            String callbackUrl = (String) request.get("callbackUrl");
            
            Map<String, Object> result = subscriptionService.initializeTransactionWithPlan(user, plan, channels, callbackUrl);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error initializing transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to initialize transaction"));
        }
    }

    /**
     * Initialize card-based subscription with automatic recurring billing
     */
    @PostMapping("/initialize-card-subscription")
    public ResponseEntity<?> initializeCardSubscription(
        @RequestBody Map<String, Object> request,
        Principal principal
    ) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            String planName = (String) request.get("plan");
            Subscription.SubscriptionPlan plan;
            
            try {
                plan = Subscription.SubscriptionPlan.valueOf(planName.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid plan name"));
            }
            
            if (plan == Subscription.SubscriptionPlan.FREE) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot subscribe to free plan"));
            }
            
            // Force card-only for recurring subscriptions
            String[] channels = {"card"};
            
            Map<String, Object> result = subscriptionService.initializeTransactionWithPlan(user, plan, channels);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error initializing card subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to initialize card subscription"));
        }
    }

    /**
     * Disable/Cancel current subscription
     */
    @PostMapping("/disable")
    public ResponseEntity<?> disableSubscription(Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            Map<String, Object> result = subscriptionService.disableSubscription(user);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error disabling subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to disable subscription"));
        }
    }
    
    /**
     * Fetch subscription details from Paystack
     */
    @GetMapping("/fetch/{subscriptionCode}")
    public ResponseEntity<?> fetchSubscription(
        @PathVariable String subscriptionCode,
        Principal principal
    ) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            Map<String, Object> result = subscriptionService.fetchSubscriptionFromPaystack(subscriptionCode);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error fetching subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch subscription"));
        }
    }
    
    /**
     * Enable a disabled subscription
     */
    @PostMapping("/enable")
    public ResponseEntity<?> enableSubscription(Principal principal) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            Map<String, Object> result = subscriptionService.enableSubscription(user);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error enabling subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to enable subscription"));
        }
    }
    
    /**
     * Test endpoint to verify authentication is working
     */
    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuth(Principal principal) {
        try {
            String email = principal.getName();
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", true);
            response.put("email", email);
            response.put("message", "Authentication working correctly");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in test auth", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Authentication test failed"));
        }
    }
    
    /**
     * Verify subscription payment callback
     */
    @GetMapping("/verify/{reference}")
    public ResponseEntity<?> verifySubscription(
        @PathVariable String reference,
        Principal principal
    ) {
        try {
            String email = principal.getName();
            User user = userService.findByEmail(email);
            
            // Verify the transaction with Paystack and update subscription
            Map<String, Object> result = subscriptionService.verifyTransactionAndUpdateSubscription(user, reference);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
            
        } catch (Exception e) {
            log.error("Error verifying subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to verify subscription"));
        }
    }
}