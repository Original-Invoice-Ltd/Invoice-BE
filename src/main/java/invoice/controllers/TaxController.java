package invoice.controllers;

import invoice.data.constants.CustomerType;
import invoice.data.constants.TaxType;
import invoice.dtos.request.TaxRequest;
import invoice.dtos.response.TaxResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.TaxService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tax")
@AllArgsConstructor
public class TaxController {
    private final TaxService taxService;

    @PostMapping("/add")
    public ResponseEntity<?> addTax(@RequestBody TaxRequest request) {
        try{
            TaxResponse response = taxService.addTax(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateTax(@RequestParam UUID id, @RequestBody TaxRequest request) {
        try{
            TaxResponse response = taxService.updateTax(id,request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/by-id")
    public ResponseEntity<?> findById(@RequestParam UUID id) {
        try{
            TaxResponse response = taxService.findById(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> findAll() {
        try{
            List<TaxResponse> response = taxService.findAll();
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("/active")
    public ResponseEntity<?> findActiveTaxes() {
        try{
            List<TaxResponse> response = taxService.findActiveTaxes();
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("/by-type")
    public ResponseEntity<?> findByTaxType(@RequestParam TaxType taxType) {
        try{
            List<TaxResponse> response = taxService.findByTaxType(taxType);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("/applicable")
    public ResponseEntity<?> findApplicableTaxes(@RequestParam CustomerType clientType) {
        try{
            List<TaxResponse> response = taxService.findApplicableTaxes(clientType);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
    
    @PostMapping("/initialize-defaults")
    public ResponseEntity<?> initializeDefaultTaxes() {
        try{
            taxService.initializeDefaultTaxes();
            return new ResponseEntity<>("Default taxes initialized successfully", HttpStatus.OK);
        }
        catch (Exception ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAll() {
        try{
            String response = taxService.deleteAll();
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete-by-id")
    public ResponseEntity<?> delete(@RequestParam UUID id) {
        try{
            String response = taxService.deleteById(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
