package invoice.controllers;

import invoice.dtos.request.TelegramMessageRequest;
import invoice.security.services.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor

public class TelegramController {
    private final TelegramService telegramService;

    @PostMapping("/sendText")
    public ResponseEntity<?>  sendText(@RequestBody TelegramMessageRequest request){
        telegramService.sendMessageAndFile(request.getUserName(), request.getFileUrl(),request.getMessageText());
        return new ResponseEntity<>(Map.of("isSuccess", true), OK);
    }
    @GetMapping("/isConnected")
    public ResponseEntity<?> hasTelegramConnected(@RequestParam("username") String username){
        return new ResponseEntity<>(Map.of("isSuccess", true, "data",Map.of("isConnected",telegramService.hasTelegramConnected(username))), OK);
    }
}