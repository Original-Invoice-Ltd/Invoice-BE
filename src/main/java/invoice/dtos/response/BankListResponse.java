package invoice.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BankListResponse {
    private boolean status;
    private String message;
    private List<Bank> data;

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Bank {
        private String name;
        private String code;
    }

}