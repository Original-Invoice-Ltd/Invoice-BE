package invoice.dtos.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
public class CreateInvoiceRequest {
    private String fullName;
    private String email;
    private String address;//optional
    private String phone;
    private String businessName;
    private String title;
    private String invoiceNumber;
    private MultipartFile logo;//optional
    private MultipartFile signature;//optional
    private LocalDate invoiceDate;
    private UUID clientId;
    private LocalDate dueDate;
    private String currency;
    private String invoiceColor;
    private String paymentTerms;
    private String accountNumber;
    private String accountName;
    private String bank;
    private String language = "English";
    private Double subtotal;
    private Double totalDue;
    private String note;//large text optional
    private String termsAndConditions;//large text optional
    private List<Long>itemIds;//optional
    private List<InvoiceItemRequest> items; // Invoice items to be created

}
