package invoice.dtos.request;

import invoice.data.constants.BusinessType;
import invoice.data.constants.Country;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BusinessProfileDto {
    private String businessName;
    private String businessFullName;
    private String registeredBusinessAddress;
    private String emailAddress;
    private String phoneNumber;
    private BusinessType businessType;
    private Country country;
    private String businessRegistrationNumber;
    private String businessLogoUrl;
}
