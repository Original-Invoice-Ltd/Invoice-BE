package invoice.dtos.response;

import invoice.data.models.Tax;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TaxResponse {
    private UUID id;
    private String name;
    private double taxRate;
    public TaxResponse(Tax tax) {
        this.id = tax.getId();
        this.name = tax.getName();
        this.taxRate = tax.getTaxRate();
    }
}
