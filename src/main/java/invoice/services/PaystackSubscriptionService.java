package invoice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import invoice.config.AppProperties;
import invoice.data.models.Subscription;
import invoice.data.models.User;
import invoice.data.repositories.SubscriptionRepository;
import invoice.data.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackSubscriptionService {
    
    private final AppProperties appProperties;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String PAYSTACK_API_URL = "https://api.paystack.co";
    
    /**
     * Helper method to make Paystack API calls with retry logic for Cloudflare issues
     */
    private ResponseEntity<String> makePaystackApiCall(String endpoint, HttpMethod method, HttpEntity<?> request, int maxRetries) {
        int attempts = 0;
        long delay = 1000; // Start with 1 second delay
        
        while (attempts < maxRetries) {
            try {
                return restTemplate.exchange(PAYSTACK_API_URL + endpoint, method, request, String.class);
            } catch (HttpClientErrorException.Forbidden e) {
                attempts++;
                String errorBody = e.getResponseBodyAsString();
                
                // Check if it's a Cloudflare block
                if (errorBody.contains("Cloudflare") && errorBody.contains("blocked")) {
                    log.warn("Cloudflare blocked request to {} (attempt {}/{}). Retrying in {}ms...", 
                        endpoint, attempts, maxRetries, delay);
                    
                    if (attempts < maxRetries) {
                        try {
                            Thread.sleep(delay);
                            delay *= 2; // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Request interrupted", ie);
                        }
                    } else {
                        log.error("All retry attempts failed for Paystack API call to {}. Cloudflare is persistently blocking this IP.", endpoint);
                        
                        // For development: Return a mock response to allow testing
                        if (endpoint.equals("/transaction/initialize") && appProperties.isPaystackMockMode()) {
                            log.warn("MOCK MODE: Returning mock Paystack response due to Cloudflare blocking");
                            String mockResponse = """
                                {
                                    "status": true,
                                    "message": "Authorization URL created (MOCK - Cloudflare blocked real API)",
                                    "data": {
                                        "authorization_url": "https://checkout.paystack.com/mock-cloudflare-blocked",
                                        "access_code": "mock_access_code_cloudflare_blocked",
                                        "reference": "mock_ref_%d"
                                    }
                                }
                                """.formatted(System.currentTimeMillis());
                            return ResponseEntity.ok(mockResponse);
                        }
                        
                        throw e;
                    }
                } else {
                    // Not a Cloudflare issue, don't retry
                    throw e;
                }
            } catch (Exception e) {
                // For other exceptions, don't retry
                throw e;
            }
        }
        
        throw new RuntimeException("Max retries exceeded for Paystack API call to " + endpoint);
    }
    
    /**
     * Initialize a subscription for a user using Paystack transaction with plan code
     * This is the recommended approach - customer pays first, then gets subscribed
     */
    public Map<String, Object> initializeSubscription(User user, Subscription.SubscriptionPlan plan) {
        try {
            String planCode = getPlanCodeForPlan(plan);
            
            if (planCode == null) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "Invalid plan selected");
                return errorResult;
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(appProperties.getPaystackSecretKey());
            headers.set("User-Agent", "Mozilla/5.0 (compatible; OriginalInvoice/1.0; +https://originalinvoice.com)");
            headers.set("Accept", "application/json");
            headers.set("Cache-Control", "no-cache");
            headers.set("Connection", "keep-alive");
            
            // Initialize transaction with plan code
            // Amount will be overridden by plan amount
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", user.getEmail());
            requestBody.put("amount", plan.getAmountInKobo());
            requestBody.put("plan", planCode);
            requestBody.put("callback_url", appProperties.getFrontend().getDashboardUrl() + "/subscription/callback");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                PAYSTACK_API_URL + "/transaction/initialize",
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                if (jsonResponse.get("status").asBoolean()) {
                    JsonNode data = jsonResponse.get("data");
                    
                    // Create pending subscription record
                    Subscription subscription = subscriptionRepository.findByUser(user)
                        .orElse(new Subscription());
                    
                    subscription.setUser(user);
                    subscription.setPlan(plan);
                    subscription.setStatus(Subscription.SubscriptionStatus.INACTIVE); // Will be activated on payment
                    subscription.setLastResetDate(LocalDateTime.now());
                    
                    subscriptionRepository.save(subscription);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("authorization_url", data.get("authorization_url").asText());
                    result.put("access_code", data.get("access_code").asText());
                    result.put("reference", data.get("reference").asText());
                    return result;
                }
            }
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to initialize subscription");
            return errorResult;
            
        } catch (Exception e) {
            log.error("Error initializing subscription", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Get subscription details for a user
     */
    public Optional<Subscription> getUserSubscription(User user) {
        return subscriptionRepository.findByUser(user);
    }
    
    /**
     * Check if user can create an invoice based on their plan
     */
    public boolean canCreateInvoice(User user) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        
        if (subscriptionOpt.isEmpty()) {
            // Create free subscription for new users
            createFreeSubscription(user);
            return true; // Free plan allows 3 invoices
        }
        
        Subscription subscription = subscriptionOpt.get();
        resetMonthlyLimitsIfNeeded(subscription);
        
        if (subscription.getPlan().isUnlimitedInvoices()) {
            return true;
        }
        
        return subscription.getInvoicesUsedThisMonth() < subscription.getPlan().getInvoiceLimit();
    }
    
    /**
     * Increment invoice usage count
     */
    public void incrementInvoiceUsage(User user) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        
        if (subscriptionOpt.isEmpty()) {
            createFreeSubscription(user);
            subscriptionOpt = subscriptionRepository.findByUser(user);
        }
        
        subscriptionOpt.ifPresent(subscription -> {
            resetMonthlyLimitsIfNeeded(subscription);
            subscription.setInvoicesUsedThisMonth(subscription.getInvoicesUsedThisMonth() + 1);
            subscriptionRepository.save(subscription);
        });
    }
    
    /**
     * Check if user can upload a logo
     */
    public boolean canUploadLogo(User user) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        
        if (subscriptionOpt.isEmpty()) {
            return false; // Free plan doesn't allow logo uploads
        }
        
        Subscription subscription = subscriptionOpt.get();
        resetMonthlyLimitsIfNeeded(subscription);
        
        if (subscription.getPlan().isUnlimitedLogos()) {
            return true;
        }
        
        return subscription.getLogosUploadedThisMonth() < subscription.getPlan().getLogoLimit();
    }
    
    /**
     * Increment logo upload count
     */
    public void incrementLogoUsage(User user) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        
        subscriptionOpt.ifPresent(subscription -> {
            resetMonthlyLimitsIfNeeded(subscription);
            subscription.setLogosUploadedThisMonth(subscription.getLogosUploadedThisMonth() + 1);
            subscriptionRepository.save(subscription);
        });
    }
    
    /**
     * Check if sharing features are enabled for user
     */
    public boolean isSharingEnabled(User user) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
        
        if (subscriptionOpt.isEmpty()) {
            return false; // Free plan doesn't have sharing
        }
        
        return subscriptionOpt.get().getPlan().isSharingEnabled();
    }
    
    /**
     * Cancel a subscription
     */
    public Map<String, Object> cancelSubscription(User user) {
        try {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
            
            if (subscriptionOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "No active subscription found");
                return result;
            }
            
            Subscription subscription = subscriptionOpt.get();
            
            // Call Paystack API to disable subscription
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(appProperties.getPaystackSecretKey());
            headers.set("User-Agent", "Mozilla/5.0 (compatible; OriginalInvoice/1.0; +https://originalinvoice.com)");
            headers.set("Accept", "application/json");
            headers.set("Cache-Control", "no-cache");
            headers.set("Connection", "keep-alive");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("code", subscription.getPaystackSubscriptionCode());
            requestBody.put("token", subscription.getPaystackEmailToken());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                PAYSTACK_API_URL + "/subscription/disable",
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
                subscription.setCancelledAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Subscription cancelled successfully");
                return result;
            }
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to cancel subscription");
            return errorResult;
            
        } catch (Exception e) {
            log.error("Error cancelling subscription", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Handle webhook events from Paystack
     */
    public void handleWebhookEvent(Map<String, Object> event) {
        String eventType = (String) event.get("event");
        
        log.info("Processing webhook event: {}", eventType);
        
        switch (eventType) {
            case "subscription.create":
                handleSubscriptionCreated(event);
                break;
            case "subscription.disable":
                handleSubscriptionDisabled(event);
                break;
            case "subscription.not_renew":
                handleSubscriptionNotRenew(event);
                break;
            case "invoice.payment_failed":
                handlePaymentFailed(event);
                break;
            case "invoice.create":
                handleInvoiceCreate(event);
                break;
            case "invoice.update":
                handleInvoiceUpdate(event);
                break;
            case "charge.success":
                handleChargeSuccess(event);
                break;
            default:
                log.info("Unhandled webhook event: {}", eventType);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleSubscriptionCreated(Map<String, Object> event) {
        try {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            String subscriptionCode = (String) data.get("subscription_code");
            String emailToken = (String) data.get("email_token");
            
            Map<String, Object> customer = (Map<String, Object>) data.get("customer");
            String email = (String) customer.get("email");
            
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("User not found for subscription: {}", email);
                return;
            }
            
            User user = userOpt.get();
            Subscription subscription = subscriptionRepository.findByUser(user)
                .orElse(new Subscription());
            
            subscription.setUser(user);
            subscription.setPaystackSubscriptionCode(subscriptionCode);
            subscription.setPaystackEmailToken(emailToken);
            subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodStart(LocalDateTime.now());
            subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
            
            // Parse next payment date
            String nextPaymentDateStr = (String) data.get("next_payment_date");
            if (nextPaymentDateStr != null) {
                subscription.setNextPaymentDate(
                    ZonedDateTime.parse(nextPaymentDateStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
                );
            }
            
            subscriptionRepository.save(subscription);
            log.info("Subscription activated for user: {}", email);
            
        } catch (Exception e) {
            log.error("Error handling subscription.create event", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleSubscriptionDisabled(Map<String, Object> event) {
        try {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            String subscriptionCode = (String) data.get("subscription_code");
            
            Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findByPaystackSubscriptionCode(subscriptionCode);
            
            if (subscriptionOpt.isEmpty()) {
                log.warn("Subscription not found: {}", subscriptionCode);
                return;
            }
            
            Subscription subscription = subscriptionOpt.get();
            subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            log.info("Subscription disabled: {}", subscriptionCode);
            
        } catch (Exception e) {
            log.error("Error handling subscription.disable event", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleSubscriptionNotRenew(Map<String, Object> event) {
        try {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            String subscriptionCode = (String) data.get("subscription_code");
            
            Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findByPaystackSubscriptionCode(subscriptionCode);
            
            if (subscriptionOpt.isEmpty()) {
                log.warn("Subscription not found: {}", subscriptionCode);
                return;
            }
            
            Subscription subscription = subscriptionOpt.get();
            subscription.setStatus(Subscription.SubscriptionStatus.NON_RENEWING);
            subscriptionRepository.save(subscription);
            
            log.info("Subscription set to non-renewing: {}", subscriptionCode);
            
        } catch (Exception e) {
            log.error("Error handling subscription.not_renew event", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handlePaymentFailed(Map<String, Object> event) {
        try {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            Map<String, Object> subscriptionData = (Map<String, Object>) data.get("subscription");
            String subscriptionCode = (String) subscriptionData.get("subscription_code");
            
            Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findByPaystackSubscriptionCode(subscriptionCode);
            
            if (subscriptionOpt.isEmpty()) {
                log.warn("Subscription not found: {}", subscriptionCode);
                return;
            }
            
            Subscription subscription = subscriptionOpt.get();
            subscription.setStatus(Subscription.SubscriptionStatus.ATTENTION);
            subscriptionRepository.save(subscription);
            
            log.warn("Payment failed for subscription: {}", subscriptionCode);
            
        } catch (Exception e) {
            log.error("Error handling invoice.payment_failed event", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleInvoiceCreate(Map<String, Object> event) {
        log.info("Invoice create event received: {}", event);
    }
    
    @SuppressWarnings("unchecked")
    private void handleInvoiceUpdate(Map<String, Object> event) {
        try {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            String status = (String) data.get("status");
            
            if ("success".equals(status)) {
                Map<String, Object> subscriptionData = (Map<String, Object>) data.get("subscription");
                String subscriptionCode = (String) subscriptionData.get("subscription_code");
                
                Optional<Subscription> subscriptionOpt = subscriptionRepository
                    .findByPaystackSubscriptionCode(subscriptionCode);
                
                if (subscriptionOpt.isPresent()) {
                    Subscription subscription = subscriptionOpt.get();
                    subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                    
                    // Update next payment date
                    String nextPaymentDateStr = (String) subscriptionData.get("next_payment_date");
                    if (nextPaymentDateStr != null) {
                        subscription.setNextPaymentDate(
                            ZonedDateTime.parse(nextPaymentDateStr, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime()
                        );
                    }
                    
                    subscriptionRepository.save(subscription);
                    log.info("Subscription payment successful: {}", subscriptionCode);
                }
            }
        } catch (Exception e) {
            log.error("Error handling invoice.update event", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleChargeSuccess(Map<String, Object> event) {
        try {
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            Map<String, Object> plan = (Map<String, Object>) data.get("plan");
            
            // Check if this charge is for a subscription
            if (plan != null && !plan.isEmpty()) {
                Map<String, Object> customer = (Map<String, Object>) data.get("customer");
                String email = (String) customer.get("email");
                
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
                    
                    if (subscriptionOpt.isPresent()) {
                        Subscription subscription = subscriptionOpt.get();
                        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                        subscriptionRepository.save(subscription);
                        log.info("Subscription activated via charge.success for user: {}", email);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error handling charge.success event", e);
        }
    }
    
    private void createFreeSubscription(User user) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(Subscription.SubscriptionPlan.FREE);
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
        subscription.setInvoicesUsedThisMonth(0);
        subscription.setLogosUploadedThisMonth(0);
        subscription.setLastResetDate(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }
    
    private void resetMonthlyLimitsIfNeeded(Subscription subscription) {
        LocalDateTime lastReset = subscription.getLastResetDate();
        LocalDateTime now = LocalDateTime.now();
        
        if (lastReset == null || lastReset.plusMonths(1).isBefore(now)) {
            subscription.setInvoicesUsedThisMonth(0);
            subscription.setLogosUploadedThisMonth(0);
            subscription.setLastResetDate(now);
            subscription.setCurrentPeriodStart(now);
            subscription.setCurrentPeriodEnd(now.plusMonths(1));
            subscriptionRepository.save(subscription);
        }
    }
    
    private String getPlanCodeForPlan(Subscription.SubscriptionPlan plan) {
        // Plan codes from Paystack dashboard
        // PLN_1hgx8jfrfx39u44 = Essentials
        // PLN_8y3o8kp1wbe5h1j = Premium
        switch (plan) {
            case ESSENTIALS:
                return appProperties.getPaystackEssentialsPlanCode();
            case PREMIUM:
                return appProperties.getPaystackPremiumPlanCode();
            default:
                return null;
        }
    }
    
    /**
     * Initialize transaction with plan code for subscription
     * This creates a subscription when customer completes payment
     */
    public Map<String, Object> initializeTransactionWithPlan(User user, Subscription.SubscriptionPlan plan, String[] channels, String callbackUrl) {
        try {
            String planCode = getPlanCodeForPlan(plan);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(appProperties.getPaystackSecretKey());
            headers.set("User-Agent", "Mozilla/5.0 (compatible; OriginalInvoice/1.0; +https://originalinvoice.com)");
            headers.set("Accept", "application/json");
            headers.set("Cache-Control", "no-cache");
            headers.set("Connection", "keep-alive");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", user.getEmail());
            requestBody.put("amount", String.valueOf(plan.getAmountInKobo()));
            requestBody.put("plan", planCode);
            
            // Add callback URL if provided, otherwise use default
            if (callbackUrl != null && !callbackUrl.isEmpty()) {
                requestBody.put("callback_url", callbackUrl);
            } else {
                requestBody.put("callback_url", appProperties.getFrontend().getDashboardUrl() + "/subscription/success");
            }
            
            // Add channels if provided
            if (channels != null && channels.length > 0) {
                requestBody.put("channels", channels);
            }
            
            log.info("Initializing Paystack transaction with plan: {}, email: {}, amount: {}, channels: {}, callback: {}", 
                planCode, user.getEmail(), plan.getAmountInKobo(), 
                channels != null ? String.join(",", channels) : "default",
                callbackUrl != null ? callbackUrl : "default");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = makePaystackApiCall(
                "/transaction/initialize",
                HttpMethod.POST,
                request,
                3 // Max 3 retry attempts
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                if (jsonResponse.get("status").asBoolean()) {
                    JsonNode data = jsonResponse.get("data");
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("authorizationUrl", data.get("authorization_url").asText());
                    result.put("accessCode", data.get("access_code").asText());
                    result.put("reference", data.get("reference").asText());
                    return result;
                }
            }
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to initialize transaction");
            return errorResult;
            
        } catch (Exception e) {
            log.error("Error initializing transaction with plan", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Overloaded method for backward compatibility - calls the main method with null channels and callback
     */
    public Map<String, Object> initializeTransactionWithPlan(User user, Subscription.SubscriptionPlan plan) {
        return initializeTransactionWithPlan(user, plan, null, null);
    }
    
    /**
     * Overloaded method for backward compatibility - calls the main method with null callback
     */
    public Map<String, Object> initializeTransactionWithPlan(User user, Subscription.SubscriptionPlan plan, String[] channels) {
        return initializeTransactionWithPlan(user, plan, channels, null);
    }
    
    /**
     * Fetch subscription details from Paystack
     */
    public Map<String, Object> fetchSubscriptionFromPaystack(String subscriptionCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(appProperties.getPaystackSecretKey());
            headers.set("User-Agent", "Mozilla/5.0 (compatible; OriginalInvoice/1.0; +https://originalinvoice.com)");
            headers.set("Accept", "application/json");
            headers.set("Cache-Control", "no-cache");
            headers.set("Connection", "keep-alive");
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                PAYSTACK_API_URL + "/subscription/" + subscriptionCode,
                HttpMethod.GET,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                if (jsonResponse.get("status").asBoolean()) {
                    JsonNode data = jsonResponse.get("data");
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("data", objectMapper.convertValue(data, Map.class));
                    return result;
                }
            }
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to fetch subscription");
            return errorResult;
            
        } catch (Exception e) {
            log.error("Error fetching subscription from Paystack", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Enable a disabled subscription
     */
    public Map<String, Object> enableSubscription(User user) {
        try {
            Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser(user);
            
            if (subscriptionOpt.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "No subscription found");
                return result;
            }
            
            Subscription subscription = subscriptionOpt.get();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(appProperties.getPaystackSecretKey());
            headers.set("User-Agent", "Mozilla/5.0 (compatible; OriginalInvoice/1.0; +https://originalinvoice.com)");
            headers.set("Accept", "application/json");
            headers.set("Cache-Control", "no-cache");
            headers.set("Connection", "keep-alive");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("code", subscription.getPaystackSubscriptionCode());
            requestBody.put("token", subscription.getPaystackEmailToken());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                PAYSTACK_API_URL + "/subscription/enable",
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                if (jsonResponse.get("status").asBoolean()) {
                    subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                    subscriptionRepository.save(subscription);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("message", "Subscription enabled successfully");
                    return result;
                }
            }
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to enable subscription");
            return errorResult;
            
        } catch (Exception e) {
            log.error("Error enabling subscription", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Verify transaction and update subscription status
     */
    public Map<String, Object> verifyTransactionAndUpdateSubscription(User user, String reference) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(appProperties.getPaystackSecretKey());
            headers.set("User-Agent", "Mozilla/5.0 (compatible; OriginalInvoice/1.0; +https://originalinvoice.com)");
            headers.set("Accept", "application/json");
            headers.set("Cache-Control", "no-cache");
            headers.set("Connection", "keep-alive");
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                PAYSTACK_API_URL + "/transaction/verify/" + reference,
                HttpMethod.GET,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                if (jsonResponse.get("status").asBoolean()) {
                    JsonNode data = jsonResponse.get("data");
                    String status = data.get("status").asText();
                    
                    if ("success".equals(status)) {
                        // Check if this transaction has a plan (subscription)
                        JsonNode planNode = data.get("plan");
                        if (planNode != null && !planNode.isNull()) {
                            String planCode = planNode.get("plan_code").asText();
                            
                            // Determine which plan this is
                            Subscription.SubscriptionPlan plan = getPlanFromCode(planCode);
                            if (plan != null) {
                                // Update or create subscription
                                Subscription subscription = subscriptionRepository.findByUser(user)
                                    .orElse(new Subscription());
                                
                                subscription.setUser(user);
                                subscription.setPlan(plan);
                                subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                                subscription.setCurrentPeriodStart(LocalDateTime.now());
                                subscription.setCurrentPeriodEnd(LocalDateTime.now().plusMonths(1));
                                subscription.setLastResetDate(LocalDateTime.now());
                                subscription.setInvoicesUsedThisMonth(0);
                                subscription.setLogosUploadedThisMonth(0);
                                
                                subscriptionRepository.save(subscription);
                                
                                Map<String, Object> result = new HashMap<>();
                                result.put("success", true);
                                result.put("message", "Subscription activated successfully");
                                result.put("plan", plan.name());
                                result.put("planDisplayName", plan.getDisplayName());
                                return result;
                            }
                        }
                        
                        // If no plan found, still return success for regular transaction
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("message", "Transaction verified successfully");
                        return result;
                    } else {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("success", false);
                        errorResult.put("message", "Transaction was not successful: " + status);
                        return errorResult;
                    }
                }
            }
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to verify transaction");
            return errorResult;
            
        } catch (Exception e) {
            log.error("Error verifying transaction", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Get subscription plan from Paystack plan code
     */
    private Subscription.SubscriptionPlan getPlanFromCode(String planCode) {
        if (planCode.equals(appProperties.getPaystackEssentialsPlanCode())) {
            return Subscription.SubscriptionPlan.ESSENTIALS;
        } else if (planCode.equals(appProperties.getPaystackPremiumPlanCode())) {
            return Subscription.SubscriptionPlan.PREMIUM;
        }
        return null;
    }
    
    /**
     * List all subscriptions for a user
     */
    public Map<String, Object> listSubscriptions() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(appProperties.getPaystackSecretKey());
            headers.set("User-Agent", "Mozilla/5.0 (compatible; OriginalInvoice/1.0; +https://originalinvoice.com)");
            headers.set("Accept", "application/json");
            headers.set("Cache-Control", "no-cache");
            headers.set("Connection", "keep-alive");
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                PAYSTACK_API_URL + "/subscription",
                HttpMethod.GET,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                if (jsonResponse.get("status").asBoolean()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("data", objectMapper.convertValue(jsonResponse.get("data"), Map.class));
                    result.put("meta", objectMapper.convertValue(jsonResponse.get("meta"), Map.class));
                    return result;
                }
            }
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to list subscriptions");
            return errorResult;
            
        } catch (Exception e) {
            log.error("Error listing subscriptions", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }
}
