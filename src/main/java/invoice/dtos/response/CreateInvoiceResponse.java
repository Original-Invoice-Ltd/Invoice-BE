package invoice.dtos.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class CreateInvoiceResponse {
    private Long id;
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
