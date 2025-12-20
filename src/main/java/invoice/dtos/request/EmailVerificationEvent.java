package invoice.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmailVerificationEvent {
    private String email;
    private String firstName;
    private String verificationToken;
    private String frontendUrl;


}