package invoice.controllers;

import invoice.dtos.request.ClientRequest;
import invoice.dtos.response.ClientResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.ClientService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/client")
@AllArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @PostMapping("/add")
    public ResponseEntity<?> addClient(Principal principal, @RequestBody ClientRequest clientRequest) {
        try{
            String email = principal.getName();
            String response = clientService.addClient(email,clientRequest);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateClient(@RequestParam UUID id, @RequestBody ClientRequest clientRequest) {
        try{
            String response = clientService.updateClient(id,clientRequest);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @GetMapping("/find-by-id")
    public ResponseEntity<?> getClient(@RequestParam UUID id) {
        try{
            ClientResponse response = clientService.getClient(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllClients() {
        try{
            List<ClientResponse> response = clientService.getAllClients();
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all-user")
    public ResponseEntity<?> getAllUserClients(Principal principal) {
        try{
            String email = principal.getName();
            List<ClientResponse> response = clientService.getAllUserClients(email);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/all-user")
    public ResponseEntity<?> deleteAllUserClients(Principal principal) {
        try{
            String email = principal.getName();
            String response = clientService.deleteAllUserClients(email);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllClients() {
        try{
            String response = clientService.deleteAllClients();
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete-by-id")
    public ResponseEntity<?> deleteClient(@RequestParam UUID id) {
        try{
            String response = clientService.deleteClient(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

}
