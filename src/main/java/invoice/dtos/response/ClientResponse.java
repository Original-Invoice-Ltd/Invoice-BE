package invoice.dtos.response;

import invoice.data.models.Client;
import invoice.data.models.InvoiceRecipient;
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
public class ClientResponse {
    private UUID id;
    private String customerType;
    private String title;
    private String fullName;
    private String businessName;
    private String phone;
    private String email;
    private String country;

    public ClientResponse(Client client){
        this.id = client.getId();
        this.businessName = client.getBusinessName();
        this.customerType = client.getCustomerType() != null ? 
            String.valueOf(client.getCustomerType()) : "INDIVIDUAL";
        this.title = client.getTitle();
        this.fullName = client.getFullName();
        this.phone = client.getPhone();
        this.email = client.getEmail();
        this.country = client.getCountry() != null? client.getCountry() : "";
   }

    public ClientResponse(InvoiceRecipient recipient){
        this.id = null; // No ID for recipients since they're not separate entities
        this.businessName = recipient.getBusinessName();
        this.customerType = recipient.getCustomerType();
        this.title = recipient.getTitle();
        this.fullName = recipient.getFullName();
        this.phone = recipient.getPhone();
        this.email = recipient.getEmail();
        this.country = recipient.getCountry() != null? recipient.getCountry() : "";
   }
}
