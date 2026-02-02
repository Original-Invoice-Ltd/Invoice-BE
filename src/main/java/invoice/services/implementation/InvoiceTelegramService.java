package invoice.services.implementation;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import invoice.data.models.TelegramUser;
import invoice.data.repositories.TelegramUserRepository;
import invoice.exception.BusinessException;
import invoice.services.TelegramService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceTelegramService implements TelegramService {

    private final TelegramBot telegramBot;
    private final TelegramUserRepository telegramUserRepository;

    @PostConstruct
    @Override
    public void init() {
        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                if (update.message() == null || update.message().text() == null) continue;
                Long chatId = update.message().chat().id();
                String text = update.message().text().trim();
                String username = update.message().chat().username();
                if (text.startsWith("/start")) handleStartCommand(chatId, username);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void handleStartCommand(Long chatId, String username) {
        telegramUserRepository.findByChatId(chatId)
                .ifPresentOrElse(
                        user -> {
                            sendTelegramMessage(chatId, "Previously connected");
                            if (username != null && !username.equals(user.getUsername())) {
                                user.setUsername(username);
                                telegramUserRepository.save(user);
                            }
                        },
                        () -> {
                            TelegramUser newUser = TelegramUser.builder().username(username).chatId(chatId).build();
                            telegramUserRepository.save(newUser);
                            sendTelegramMessage(chatId, "Telegram connected successfully, You can now receive invoices and updates from this bot.");
                        }
                );
    }

    private void sendTelegramMessage(long chatId, String text) {
        telegramBot.execute(new SendMessage(chatId, text));
    }

    @Override
    public void sendDocument(String username, String fileUrl) {
        TelegramUser telegramUser = telegramUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BusinessException("User has not connected Telegram"));
        telegramBot.execute(new SendDocument(telegramUser.getChatId(), fileUrl));
    }

    @Override
    public void sendMessageAndFile(String username, String fileUrl, String text) {
        try{
            TelegramUser telegramUser = telegramUserRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new BusinessException("User has not connected Telegram"));
            sendTelegramMessage(telegramUser.getChatId(),text);
            sendDocument(telegramUser.getUsername(), fileUrl);
        }catch(Exception e){
            throw new BusinessException("Unable to sendFileAndMessage");
        }
    }
    @Override
    public boolean hasTelegramConnected(String username){
        Optional<TelegramUser> user = telegramUserRepository.findByUsernameIgnoreCase(username);
        return user.isPresent();
    }
}