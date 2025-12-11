package invoice.controllers;

import invoice.data.models.Tax;
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
    public ResponseEntity<?> addTax(@RequestParam UUID id, @RequestBody TaxRequest request) {
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
