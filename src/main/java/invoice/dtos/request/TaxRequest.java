package invoice.dtos.request;

import invoice.data.constants.TaxType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TaxRequest {
    private String name;
    private TaxType taxType;
    private BigDecimal baseTaxRate;
    private BigDecimal individualRate;
    private BigDecimal businessRate;
    private String description;
    private boolean isActive = true;
}
