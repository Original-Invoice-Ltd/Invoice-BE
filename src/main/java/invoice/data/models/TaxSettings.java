package invoice.data.models;

import invoice.data.constants.TaxApplied;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaxSettings {
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaxApplied taxApplied=TaxApplied.VAT;
    @Builder.Default
    private String taxId = "";
    @Builder.Default
    private boolean isEnablingVAT=false;
    @Builder.Default
    private boolean isEnablingWHT=false;
}
