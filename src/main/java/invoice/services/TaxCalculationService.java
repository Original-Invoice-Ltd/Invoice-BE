package invoice.services;

import invoice.data.constants.CustomerType;
import invoice.data.models.InvoiceItem;
import invoice.data.models.InvoiceItemTax;
import invoice.data.models.Tax;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TaxCalculationService {
    
    /**
     * Calculate tax amount for a specific tax and item amount based on client type
     */
    BigDecimal calculateTaxAmount(Tax tax, BigDecimal itemAmount, CustomerType clientType);
    
    /**
     * Create InvoiceItemTax entities for an invoice item
     */
    List<InvoiceItemTax> calculateItemTaxes(InvoiceItem item, List<UUID> taxIds, CustomerType clientType);
    
    /**
     * Recalculate all taxes for an invoice item
     */
    void recalculateItemTaxes(InvoiceItem item, List<UUID> taxIds, CustomerType clientType);
    
    /**
     * Get default taxes based on tax type and client type
     */
    List<Tax> getDefaultTaxes(CustomerType clientType);
}