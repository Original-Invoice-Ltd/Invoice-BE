package invoice.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountValidationResponse {
    private boolean status;
    private String message;
    private AccountData data;

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountData {
        private String account_number;
        private String account_name;
        private Long bank_id;
    }
}