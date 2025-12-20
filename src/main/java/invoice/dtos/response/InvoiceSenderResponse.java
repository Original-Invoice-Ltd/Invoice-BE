package invoice.dtos.response;

import invoice.data.models.InvoiceSender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceSenderResponse {
    private Long id;
    private String fullName;
    private String email;
    private String address;//optional
    private String phone;
    private String businessName;
    public InvoiceSenderResponse(InvoiceSender invoiceSender) {
        this.id = invoiceSender.getId();
        this.fullName = invoiceSender.getFullName();
        this.email = invoiceSender.getEmail();
        this.address = invoiceSender.getAddress() != null ? invoiceSender.getAddress() : "";
        this.phone = invoiceSender.getPhone();
        this.businessName = invoiceSender.getBusinessName();
    }
}
