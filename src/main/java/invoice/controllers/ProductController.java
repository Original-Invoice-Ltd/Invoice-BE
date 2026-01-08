package invoice.controllers;

import invoice.dtos.request.ProductRequest;
import invoice.dtos.response.ProductResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/product")
@AllArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping("/add")
    public ResponseEntity<?> addProduct(Principal principal, @RequestBody ProductRequest productRequest) {
        try {
            String email = principal.getName();
            ProductResponse response = productService.addProduct(email, productRequest);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateProduct(@RequestParam UUID id, @RequestBody ProductRequest productRequest) {
        try {
            ProductResponse response = productService.updateProduct(id, productRequest);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @GetMapping("/find-by-id")
    public ResponseEntity<?> getProduct(@RequestParam UUID id) {
        try {
            ProductResponse response = productService.getProduct(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllProducts() {
        try {
            List<ProductResponse> response = productService.getAllProducts();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/all-user")
    public ResponseEntity<?> getAllUserProducts(Principal principal) {
        try {
            String email = principal.getName();
            List<ProductResponse> response = productService.getAllUserProducts(email);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/by-ids")
    public ResponseEntity<?> getProductsByIds(Principal principal, @RequestBody List<UUID> ids) {
        try {
            String email = principal.getName();
            List<ProductResponse> response = productService.getProductsByIds(ids, email);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/all-user")
    public ResponseEntity<?> deleteAllUserProducts(Principal principal) {
        try {
            String email = principal.getName();
            String response = productService.deleteAllUserProducts(email);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllProducts() {
        try {
            String response = productService.deleteAllProducts();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (OriginalInvoiceBaseException ex) {
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete-by-id")
    public ResponseEntity<?> deleteProduct(@RequestParam UUID id) {
        try {
            String response = productService.deleteProduct(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException ex) {
            // Handle both OriginalInvoiceBaseException and other RuntimeExceptions
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }
    
    @GetMapping("/can-delete")
    public ResponseEntity<?> canDeleteProduct(@RequestParam UUID id) {
        try {
            boolean canDelete = productService.canDeleteProduct(id);
            return new ResponseEntity<>(canDelete, HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(false, HttpStatus.OK);
        }
    }
}