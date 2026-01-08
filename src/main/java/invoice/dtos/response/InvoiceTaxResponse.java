package invoice.dtos.response;

import invoice.data.models.InvoiceTax;
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
public class InvoiceTaxResponse {
    private UUID taxId;
    private String taxName;
    private String taxType;
    private BigDecimal appliedRate;
    private BigDecimal taxAmount;
    private BigDecimal taxableAmount;
    
    public InvoiceTaxResponse(InvoiceTax invoiceTax) {
        if (invoiceTax != null && invoiceTax.getTax() != null) {
            this.taxId = invoiceTax.getTax().getId();
            this.taxName = invoiceTax.getTax().getName();
            this.taxType = invoiceTax.getTax().getTaxType() != null ? 
                          invoiceTax.getTax().getTaxType().toString() : null;
            this.appliedRate = invoiceTax.getAppliedRate();
            this.taxAmount = invoiceTax.getTaxAmount();
            this.taxableAmount = invoiceTax.getTaxableAmount();
        }
    }
}