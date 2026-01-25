package invoice.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TelegramMessageRequest {
    private String messageText;
    private String userName;
    private String fileUrl;
}