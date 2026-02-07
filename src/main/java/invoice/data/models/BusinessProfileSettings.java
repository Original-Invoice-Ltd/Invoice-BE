package invoice.data.models;


import invoice.data.constants.BusinessType;
import invoice.data.constants.Country;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
public class BusinessProfileSettings {
    @Builder.Default
    private String businessName = "";
    @Builder.Default
    private String businessFullName="";
    @Builder.Default
    private String registeredBusinessAddress="";
    @Builder.Default
    private String emailAddress="";
    @Builder.Default
    private String phoneNumber="";
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BusinessType businessType = BusinessType.SOLE_PROPRIETORSHIP;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Country country = Country.NONE;
    @Builder.Default
    private String businessRegistrationNumber="";
    @Builder.Default
    private String businessLogoUrl="";
}
