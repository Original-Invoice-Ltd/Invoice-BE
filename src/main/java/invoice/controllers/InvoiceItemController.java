package invoice.controllers;

import invoice.dtos.request.InvoiceItemRequest;
import invoice.dtos.response.InvoiceItemResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.InvoiceItemService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/items")
@AllArgsConstructor
public class InvoiceItemController {

    private final InvoiceItemService invoiceItemService;

    @PostMapping
    public ResponseEntity<?> addItemToInvoice(
            @PathVariable UUID invoiceId,
            @RequestBody InvoiceItemRequest request) {
        try {
            InvoiceItemResponse response = invoiceItemService.addItemToInvoice(invoiceId, request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<?> getInvoiceItems(@PathVariable UUID invoiceId) {
        try {
            List<InvoiceItemResponse> responses = invoiceItemService.getInvoiceItems(invoiceId);
            return ResponseEntity.ok(responses);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<?> getInvoiceItemById(@PathVariable Long itemId) {
        try {
            InvoiceItemResponse response = invoiceItemService.getInvoiceItemById(itemId);
            return ResponseEntity.ok(response);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateInvoiceItem(
            @PathVariable Long itemId,
            @RequestBody InvoiceItemRequest request) {
        try {
            InvoiceItemResponse response = invoiceItemService.updateInvoiceItem(itemId, request);
            return ResponseEntity.ok(response);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<?> deleteInvoiceItem(@PathVariable Long itemId) {
        try {
            invoiceItemService.deleteInvoiceItem(itemId);
            return ResponseEntity.noContent().build();
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
