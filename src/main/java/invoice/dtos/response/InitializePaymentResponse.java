// InitializePaymentResponse.java
package invoice.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitializePaymentResponse {
    private String status;
    private String message;
    private PaymentData data;
    private Double execTime;
    private Object[] error;
    private String errorMessage;

    @Data
    public static class PaymentData {
        private String authorizationUrl;
        private String reference;
        private String accessCode;
    }
}

