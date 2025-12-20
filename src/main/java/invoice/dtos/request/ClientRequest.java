package invoice.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClientRequest {
    private String customerType;
    private String title;
    private String fullName;
    private String businessName;
    private String phone;
    private String email;
    private String country;
}
