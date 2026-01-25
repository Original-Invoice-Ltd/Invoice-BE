package invoice.dtos.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WhatsappDocumentRequest {
    private String documentUrl;
    private String userEmail;
    private String receiverPhoneNumber;
    private String message;
}