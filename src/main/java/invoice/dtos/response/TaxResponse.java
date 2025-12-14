package invoice.dtos.response;

import invoice.data.constants.CustomerType;
import invoice.data.constants.TaxType;
import invoice.data.models.Tax;
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
public class TaxResponse {
    private UUID id;
    private String name;
    private TaxType taxType;
    private BigDecimal baseTaxRate;
    private BigDecimal individualRate;
    private BigDecimal businessRate;
    private String description;
    private boolean isActive;
    
    public TaxResponse(Tax tax) {
        this.id = tax.getId();
        this.name = tax.getName();
        this.taxType = tax.getTaxType();
        this.baseTaxRate = tax.getBaseTaxRate();
        this.individualRate = tax.getIndividualRate();
        this.businessRate = tax.getBusinessRate();
        this.description = tax.getDescription();
        this.isActive = tax.isActive();
    }
    
    /**
     * Get applicable rate for a specific client type
     */
    public BigDecimal getApplicableRate(CustomerType clientType) {
        if (clientType == null) {
            return baseTaxRate != null ? baseTaxRate : BigDecimal.ZERO;
        }
        
        switch (clientType) {
            case INDIVIDUAL:
                return individualRate != null ? individualRate : 
                       (baseTaxRate != null ? baseTaxRate : BigDecimal.ZERO);
            case BUSINESS:
                return businessRate != null ? businessRate : 
                       (baseTaxRate != null ? baseTaxRate : BigDecimal.ZERO);
            default:
                return baseTaxRate != null ? baseTaxRate : BigDecimal.ZERO;
        }
    }
}
