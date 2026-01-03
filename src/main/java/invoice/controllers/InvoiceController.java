package invoice.controllers;

import invoice.dtos.request.CreateInvoiceRequest;
import invoice.dtos.response.InvoiceResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.InvoiceService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/invoices")
@AllArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;

    @PostMapping(path = "/add",consumes = {"multipart/form-data"})
    public ResponseEntity<?> createInvoice(@ModelAttribute CreateInvoiceRequest request) {
        try{
            InvoiceResponse response = invoiceService.createInvoice(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(),BAD_REQUEST);
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getInvoiceById(@PathVariable UUID id) {
        try {
            InvoiceResponse response = invoiceService.getInvoiceById(id);
            return ResponseEntity.ok(response);
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(),BAD_REQUEST);
        }
    }

    @GetMapping("/all-user")
    public ResponseEntity<?> getAllUserInvoices() {
        try {
            List<InvoiceResponse> responses = invoiceService.getAllUserInvoices();
            return ResponseEntity.ok(responses);
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllInvoices() {
        try {
            List<InvoiceResponse> responses = invoiceService.getAllInvoices();
            return ResponseEntity.ok(responses);
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PatchMapping(value = "/update/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateInvoice(
            @PathVariable UUID id,
            @ModelAttribute CreateInvoiceRequest request) {
        try{
            InvoiceResponse updated = invoiceService.updateInvoice(id, request);
            return ResponseEntity.ok(updated);
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        try {
            invoiceService.deleteInvoice(id);
            return ResponseEntity.noContent().build();
        }catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}