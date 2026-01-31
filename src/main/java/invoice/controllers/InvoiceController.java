package invoice.controllers;

import invoice.dtos.request.CreateInvoiceRequest;
import invoice.dtos.response.InvoiceResponse;
import invoice.dtos.response.ReceiptResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.InvoiceService;
import invoice.services.PaystackSubscriptionService;
import invoice.services.UserService;
import invoice.data.models.User;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/invoices")
@AllArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final PaystackSubscriptionService subscriptionService;
    private final UserService userService;

    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuth(Principal principal) {
        try {
            if (principal == null) {
                return new ResponseEntity<>("No principal found", HttpStatus.UNAUTHORIZED);
            }
            return ResponseEntity.ok("Authentication working for user: " + principal.getName());
        } catch (Exception ex) {
            return new ResponseEntity<>("Error: " + ex.getMessage(), BAD_REQUEST);
        }
    }

    @PostMapping("/add-json")
    public ResponseEntity<?> createInvoiceJson(Principal principal, @RequestBody Map<String, Object> requestData) {
        try{
            // Debug logging
            System.out.println("Principal: " + (principal != null ? principal.getName() : "NULL"));
            
            if (principal == null) {
                return new ResponseEntity<>("No authentication found", HttpStatus.UNAUTHORIZED);
            }
            
            return ResponseEntity.ok("JSON endpoint working for user: " + principal.getName());
        } catch (Exception ex) {
            System.out.println("Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>("Internal server error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path = "/add", consumes = {"multipart/form-data"})
    public ResponseEntity<?> createInvoice(Principal principal, @ModelAttribute CreateInvoiceRequest request) {
        try{
            // Debug logging
            System.out.println("=== Invoice Creation Debug ===");
            System.out.println("Principal: " + (principal != null ? principal.getName() : "NULL"));
            System.out.println("Request: " + request);
            System.out.println("==============================");
            
            if (principal == null) {
                return new ResponseEntity<>("No authentication found", HttpStatus.UNAUTHORIZED);
            }
            
            // Check subscription limits before creating invoice
            User user = userService.findByEmail(principal.getName());
            if (!subscriptionService.canCreateInvoice(user)) {
                return new ResponseEntity<>(Map.of(
                    "error", "Invoice limit reached for your current plan",
                    "message", "Please upgrade your subscription to create more invoices"
                ), HttpStatus.FORBIDDEN);
            }
            
            InvoiceResponse response = invoiceService.createInvoice(request);
            
            // Increment invoice usage count after successful creation
            subscriptionService.incrementInvoiceUsage(user);
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch (OriginalInvoiceBaseException ex){
            System.out.println("Invoice creation error: " + ex.getMessage());
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        } catch (Exception ex) {
            System.out.println("Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>("Internal server error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/all-user")
    public ResponseEntity<?> getAllUserInvoices(Principal principal) {
        try {
            List<InvoiceResponse> responses = invoiceService.getAllUserInvoices();
            return ResponseEntity.ok(responses);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @GetMapping("/all-user/{userId}")
    public ResponseEntity<?> getAllUserInvoicesByUserId(@PathVariable("userId") UUID userId){
        try{
            List<InvoiceResponse> responses = invoiceService.getAllUserInvoices(userId);
            return ResponseEntity.ok(responses);
        }
        catch(OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllInvoices(Principal principal) {
        try {
            List<InvoiceResponse> responses = invoiceService.getAllInvoices();
            return ResponseEntity.ok(responses);
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInvoiceById(Principal principal, @PathVariable UUID id) {
        try {
            InvoiceResponse response = invoiceService.getInvoiceById(id);
            return ResponseEntity.ok(response);
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @PatchMapping(value = "/update/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateInvoice(
            Principal principal,
            @PathVariable UUID id,
            @ModelAttribute CreateInvoiceRequest request) {
        try{
            InvoiceResponse updated = invoiceService.updateInvoice(id, request);
            return ResponseEntity.ok(updated);
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteInvoice(Principal principal, @PathVariable UUID id) {
        try {
            invoiceService.deleteInvoice(id);
            return ResponseEntity.noContent().build();
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // Public endpoint for customers to upload payment evidence (no authentication required)
    @PostMapping(value = "/{uuid}/upload-evidence", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadPaymentEvidence(
            @PathVariable UUID uuid,
            @RequestParam("evidence") MultipartFile evidenceFile) {
        try {
            if (evidenceFile == null || evidenceFile.isEmpty()) {
                return new ResponseEntity<>("Evidence file is required", HttpStatus.BAD_REQUEST);
            }
            
            InvoiceResponse response = invoiceService.uploadPaymentEvidence(uuid, evidenceFile);
            return ResponseEntity.ok(response);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        } catch (Exception ex) {
            System.out.println("Unexpected error during evidence upload: " + ex.getMessage());
            ex.printStackTrace();
            return new ResponseEntity<>("Internal server error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Public endpoint for customers to view invoice details (no authentication required)
    @GetMapping("/public/{uuid}")
    public ResponseEntity<?> getInvoiceByUuid(@PathVariable UUID uuid) {
        try {
            InvoiceResponse response = invoiceService.getInvoiceByUuid(uuid);
            return ResponseEntity.ok(response);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @GetMapping("/stats/received")
    public ResponseEntity<?> getInvoiceStats(@RequestParam String email) {
        try {
            Map<String, Long> stats = invoiceService.getInvoiceStats(email);
            return ResponseEntity.ok(stats);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @PatchMapping("/{id}/mark-as-paid")
    public ResponseEntity<?> markInvoiceAsPaid(
            Principal principal, 
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            String paymentMethod = requestBody != null ? requestBody.get("paymentMethod") : "Bank Transfer";
            ReceiptResponse response = invoiceService.markInvoiceAsPaid(id, paymentMethod);
            return ResponseEntity.ok(response);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }
}