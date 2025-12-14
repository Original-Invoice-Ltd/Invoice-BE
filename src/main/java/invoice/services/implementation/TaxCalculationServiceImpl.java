package invoice.services.implementation;

import invoice.data.constants.CustomerType;
import invoice.data.constants.TaxType;
import invoice.data.models.InvoiceItem;
import invoice.data.models.InvoiceItemTax;
import invoice.data.models.Tax;
import invoice.data.repositories.InvoiceItemTaxRepository;
import invoice.data.repositories.TaxRepository;
import invoice.services.TaxCalculationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class TaxCalculationServiceImpl implements TaxCalculationService {
    
    private final TaxRepository taxRepository;
    private final InvoiceItemTaxRepository invoiceItemTaxRepository;
    
    @Override
    public BigDecimal calculateTaxAmount(Tax tax, BigDecimal itemAmount, CustomerType clientType) {
        if (tax == null || itemAmount == null || itemAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal applicableRate = tax.getApplicableRate(clientType);
        if (applicableRate == null || applicableRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        // Calculate tax amount: (itemAmount * taxRate) / 100
        BigDecimal taxAmount = itemAmount
                .multiply(applicableRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        log.debug("Calculated tax amount: {} for item amount: {} with rate: {}% (client type: {})", 
                taxAmount, itemAmount, applicableRate, clientType);
        
        return taxAmount;
    }
    
    @Override
    @Transactional
    public List<InvoiceItemTax> calculateItemTaxes(InvoiceItem item, List<UUID> taxIds, CustomerType clientType) {
        List<InvoiceItemTax> itemTaxes = new ArrayList<>();
        
        if (taxIds == null || taxIds.isEmpty() || item.getAmount() == null) {
            return itemTaxes;
        }
        
        for (UUID taxId : taxIds) {
            Tax tax = taxRepository.findById(taxId).orElse(null);
            if (tax == null || !tax.isActive()) {
                log.warn("Tax with ID {} not found or inactive, skipping", taxId);
                continue;
            }
            
            BigDecimal appliedRate = tax.getApplicableRate(clientType);
            BigDecimal taxAmount = calculateTaxAmount(tax, item.getAmount(), clientType);
            
            InvoiceItemTax itemTax = new InvoiceItemTax();
            itemTax.setInvoiceItem(item);
            itemTax.setTax(tax);
            itemTax.setAppliedRate(appliedRate);
            itemTax.setTaxAmount(taxAmount);
            itemTax.setTaxableAmount(item.getAmount());
            
            itemTaxes.add(itemTax);
            
            log.debug("Created item tax: {} ({}%) = {} for item: {}", 
                    tax.getName(), appliedRate, taxAmount, item.getItemName());
        }
        
        return itemTaxes;
    }
    
    @Override
    @Transactional
    public void recalculateItemTaxes(InvoiceItem item, List<UUID> taxIds, CustomerType clientType) {
        // Clear existing taxes
        item.getItemTaxes().clear();
        
        // Calculate new taxes
        List<InvoiceItemTax> newTaxes = calculateItemTaxes(item, taxIds, clientType);
        
        // Add new taxes to item
        for (InvoiceItemTax itemTax : newTaxes) {
            item.addItemTax(itemTax);
        }
        
        log.info("Recalculated taxes for item: {} - Total tax amount: {}", 
                item.getItemName(), item.getTotalTaxAmount());
    }
    
    @Override
    public List<Tax> getDefaultTaxes(CustomerType clientType) {
        // Get active taxes that have rates defined for the client type
        List<Tax> allActiveTaxes = taxRepository.findByIsActiveTrue();
        List<Tax> applicableTaxes = new ArrayList<>();
        
        for (Tax tax : allActiveTaxes) {
            BigDecimal applicableRate = tax.getApplicableRate(clientType);
            if (applicableRate != null && applicableRate.compareTo(BigDecimal.ZERO) > 0) {
                applicableTaxes.add(tax);
            }
        }
        
        log.debug("Found {} applicable taxes for client type: {}", applicableTaxes.size(), clientType);
        return applicableTaxes;
    }
}