package invoice.dtos.request;

import invoice.data.constants.TaxApplied;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxSettingsDto {
    private TaxApplied taxApplied;
    private String taxId;
    private boolean enablingVAT;
    private boolean enablingWHT;
}