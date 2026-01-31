package invoice.services;


import invoice.data.models.User;

public interface TelegramService {
    void sendDocument(String username, String fileURL);
    void sendMessageAndFile(String username, String fileUrl, String text);
    void init();
    boolean hasTelegramConnected(String username);
}
