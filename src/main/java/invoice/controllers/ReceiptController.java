package invoice.controllers;

import invoice.dtos.response.ReceiptResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.ReceiptService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/receipts")
@AllArgsConstructor
public class ReceiptController {
    
    private final ReceiptService receiptService;
    
    @GetMapping("/{uuid}")
    public ResponseEntity<?> getReceiptById(Principal principal, @PathVariable UUID uuid) {
        try {
            ReceiptResponse response = receiptService.getReceiptById(uuid);
            return ResponseEntity.ok(response);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }
    
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<?> getReceiptByInvoiceId(Principal principal, @PathVariable UUID invoiceId) {
        try {
            ReceiptResponse response = receiptService.getReceiptByInvoiceId(invoiceId);
            return ResponseEntity.ok(response);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }
    
    @GetMapping("/{uuid}/download")
    public ResponseEntity<?> downloadReceipt(@PathVariable UUID uuid) {
        try {
            byte[] pdfBytes = receiptService.generateReceiptPdf(uuid);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=receipt-" + uuid + ".pdf")
                    .body(pdfBytes);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        } catch (Exception ex) {
            return new ResponseEntity<>("Failed to generate PDF: " + ex.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
