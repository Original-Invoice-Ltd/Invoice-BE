package invoice.dtos.response;

import invoice.data.models.InvoiceItemTax;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ItemTaxResponse {
    private UUID taxId;
    private String taxName;
    private String taxType;
    private BigDecimal appliedRate;
    private BigDecimal taxAmount;
    private BigDecimal taxableAmount;
    
    public ItemTaxResponse(InvoiceItemTax itemTax) {
        if (itemTax != null && itemTax.getTax() != null) {
            this.taxId = itemTax.getTax().getId();
            this.taxName = itemTax.getTax().getName();
            this.taxType = itemTax.getTax().getTaxType() != null ? 
                          itemTax.getTax().getTaxType().toString() : null;
            this.appliedRate = itemTax.getAppliedRate();
            this.taxAmount = itemTax.getTaxAmount();
            this.taxableAmount = itemTax.getTaxableAmount();
        }
    }
}
