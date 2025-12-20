package invoice.controllers;

import invoice.data.constants.CustomerType;
import invoice.data.models.Tax;
import invoice.data.repositories.TaxRepository;
import invoice.services.TaxCalculationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tax-calculation")
@AllArgsConstructor
public class TaxCalculationController {
    
    private final TaxCalculationService taxCalculationService;
    private final TaxRepository taxRepository;
    
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateTax(
            @RequestParam UUID taxId,
            @RequestParam BigDecimal itemAmount,
            @RequestParam CustomerType clientType) {
        
        try {
            Tax tax = taxRepository.findById(taxId)
                    .orElseThrow(() -> new RuntimeException("Tax not found"));
            
            BigDecimal taxAmount = taxCalculationService.calculateTaxAmount(tax, itemAmount, clientType);
            BigDecimal applicableRate = tax.getApplicableRate(clientType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taxId", taxId);
            response.put("taxName", tax.getName());
            response.put("taxType", tax.getTaxType());
            response.put("itemAmount", itemAmount);
            response.put("clientType", clientType);
            response.put("applicableRate", applicableRate);
            response.put("calculatedTaxAmount", taxAmount);
            response.put("totalAmount", itemAmount.add(taxAmount));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error calculating tax: " + e.getMessage());
        }
    }
    
    @GetMapping("/test-rates")
    public ResponseEntity<?> testTaxRates(@RequestParam UUID taxId) {
        try {
            Tax tax = taxRepository.findById(taxId)
                    .orElseThrow(() -> new RuntimeException("Tax not found"));
            
            BigDecimal testAmount = BigDecimal.valueOf(1000); // Test with 1000 currency units
            
            Map<String, Object> response = new HashMap<>();
            response.put("taxId", taxId);
            response.put("taxName", tax.getName());
            response.put("taxType", tax.getTaxType());
            response.put("testAmount", testAmount);
            
            // Test for individual client
            BigDecimal individualRate = tax.getApplicableRate(CustomerType.INDIVIDUAL);
            BigDecimal individualTax = taxCalculationService.calculateTaxAmount(tax, testAmount, CustomerType.INDIVIDUAL);
            
            Map<String, Object> individualResult = new HashMap<>();
            individualResult.put("rate", individualRate);
            individualResult.put("taxAmount", individualTax);
            individualResult.put("totalAmount", testAmount.add(individualTax));
            
            // Test for business client
            BigDecimal businessRate = tax.getApplicableRate(CustomerType.BUSINESS);
            BigDecimal businessTax = taxCalculationService.calculateTaxAmount(tax, testAmount, CustomerType.BUSINESS);
            
            Map<String, Object> businessResult = new HashMap<>();
            businessResult.put("rate", businessRate);
            businessResult.put("taxAmount", businessTax);
            businessResult.put("totalAmount", testAmount.add(businessTax));
            
            response.put("individual", individualResult);
            response.put("business", businessResult);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error testing tax rates: " + e.getMessage());
        }
    }
}