//PaymentTransactionResponse
package invoice.dtos.response;

import invoice.data.models.PaymentTransaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentTransactionResponse {
    private UUID id;
    private String reference;
    private String email;
    private Double amount;
    private String status;
    private String paymentType;
    private LocalDateTime createdAt;

    public PaymentTransactionResponse(PaymentTransaction paymentTransaction) {
        this.id = paymentTransaction.getId();
        this.reference = paymentTransaction.getReference();
        this.email = paymentTransaction.getEmail();
        this.amount = paymentTransaction.getAmount();
        this.status = paymentTransaction.getStatus();
        this.createdAt = paymentTransaction.getCreatedAt();
    }
}
