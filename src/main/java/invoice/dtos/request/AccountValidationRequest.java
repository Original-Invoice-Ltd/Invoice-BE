package invoice.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountValidationRequest {
    @NotBlank(message = "Account number is required")
    @Size(min = 10, max = 10, message = "Account number must be 10 digits")
    private String accountNumber;
    
    @NotBlank(message = "Bank code is required")
    private String bankCode;
}