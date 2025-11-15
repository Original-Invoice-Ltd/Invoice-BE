package invoice.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class CreateInvoiceRequest {
    private String title;
    private String website;
    private String businessOwner;
    private String invoiceNumber;
    private String logoUrl;
    private String imageUrl;
    private LocalDate creationDate;
    private LocalDate dueDate;
    private String currency;
    private Double discount;
}
