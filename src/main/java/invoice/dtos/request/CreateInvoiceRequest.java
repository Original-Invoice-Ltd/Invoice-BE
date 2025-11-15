package invoice.dtos.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
public class CreateInvoiceRequest {
    private String title;
    private String website;
    private String businessOwner;
    private String invoiceNumber;
    private MultipartFile logo;
    private MultipartFile image;
    private LocalDateTime creationDate;
    private LocalDateTime dueDate;
    private String currency;
    private Double discount;
}
