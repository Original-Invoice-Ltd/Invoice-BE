package invoice.controllers;

import invoice.dtos.request.InitializePaymentRequest;
import invoice.dtos.response.*;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/initialize")
    public ResponseEntity<?> initializePayment(
            @Valid @RequestBody InitializePaymentRequest request) {
        InitializePaymentResponse response = paymentService.initializePayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestParam String reference) {
        VerifyPaymentResponse response = paymentService.verifyPayment(reference);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<?>updateTransaction(@RequestParam String reference){
        try{
            String response = paymentService.updateTransaction(reference);
            return ResponseEntity.ok(response);
        }
        catch (OriginalInvoiceBaseException ex){
            return ResponseEntity.status(BAD_REQUEST).body(ex.getMessage());
        }
    }

    @GetMapping("/allTransactions")
    public ResponseEntity<?> getAllTransactions() {
        List<PaymentTransactionResponse> response = paymentService.getTotalTransactions();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/allTransactionAmount")
    public ResponseEntity<?> getAllTransactionAmount() {
        Double totalAmount = paymentService.getTotalTransactionAmount();
        return ResponseEntity.ok(totalAmount);
    }

    @GetMapping("/transaction")
    public ResponseEntity<?> getTransaction(@RequestParam UUID id) {
        try {
            PaymentTransactionResponse response = paymentService.getTransaction(id);
            return ResponseEntity.ok(response);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @GetMapping("/allPromotion")
    public ResponseEntity<?> getAllPromotion() {
        List<PaymentTransactionResponse> transactions = paymentService.getAllPromotionsTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/allPromotionAmount")
    public ResponseEntity<?> getAllPromotionAmount() {
        Double totalAmount = paymentService.getTotalPromotionAmount();
        return ResponseEntity.ok(totalAmount);
    }
    
    @GetMapping("/callback")
    public ResponseEntity<String> paymentCallback(
            @RequestParam String status,
            @RequestParam(required = false) String errorMessage,
            @RequestParam String reference,
            @RequestParam Double transAmount,
            @RequestParam String currency) {
        
        paymentService.handlePaymentCallback(status, errorMessage, reference, transAmount, currency, reference);
        return ResponseEntity.ok("Callback processed successfully");
    }

    @GetMapping("/banks")
    public ResponseEntity<BankListResponse> getBanks() {
        BankListResponse response = paymentService.getBanks();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate-account")
    public ResponseEntity<?> validateAccount(
            @RequestParam @NotBlank String accountNumber,
            @RequestParam @NotBlank String bankCode) {
        try{
            AccountValidationResponse response = paymentService.validateAccountNumber(accountNumber, bankCode);
            return ResponseEntity.ok(response);
        }
        catch (Exception ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }

    }
}