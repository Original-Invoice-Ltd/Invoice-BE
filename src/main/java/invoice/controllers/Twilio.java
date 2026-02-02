package invoice.controllers;

import invoice.dtos.request.WhatsappDocumentRequest;
import invoice.services.WhatsappService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class Twilio {
    private final WhatsappService whatsappService;
    @PostMapping("/sendDocument")
    public ResponseEntity<?> sendDocument(@RequestBody WhatsappDocumentRequest request){
        String messageSID =  whatsappService.sendDocument(request.getUserEmail(), request.getReceiverPhoneNumber(),request.getDocumentUrl(), request.getMessage());
        return new ResponseEntity<>(
                Map.of("isSuccess", true,
                        "data", messageSID
                ),
                OK
        );
    }
}
