package invoice.services.implementation;

import com.twilio.type.PhoneNumber;
import invoice.exception.BusinessException;
import invoice.services.UserService;
import invoice.services.WhatsappService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.twilio.rest.api.v2010.account.Message;


@Service
@RequiredArgsConstructor
public class InvoiceWhatsappService implements WhatsappService {
    private final UserService userService;

    @Value("${TWILIO_PHONE_NUMBER}")
    private String invoiceWhatsappNumber;
    @Override
    public String sendDocument(String email, String receiverPhoneNumber, String url, String message) {
        validateInputs(email, receiverPhoneNumber);
        var messageCreated = Message.creator(
                new PhoneNumber("whatsapp:"+receiverPhoneNumber),
                new PhoneNumber("whatsapp:"+ invoiceWhatsappNumber),
                message.isBlank() || message.isEmpty() ? "" : message.trim()
        ).setMediaUrl(java.util.List.of(java.net.URI.create(url))).create();
        return messageCreated.getSid();
    }

    private void validateInputs(String email, String receiverPhoneNumber) {
        if(email.isEmpty()|| email.isBlank()) throw new BusinessException("Invalid Email Provided");
        if(receiverPhoneNumber==null || !receiverPhoneNumber.matches("^\\+?[1-9]\\d{6,14}$")) throw new BusinessException("Invalid Phone number Provided");
        if(!userService.existsByEmail(email)) throw new BusinessException("Invalid user Email provided");
    }

}