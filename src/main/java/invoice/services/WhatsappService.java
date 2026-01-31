package invoice.services;

public interface WhatsappService {
    String sendDocument(String email, String phoneNumber, String url, String message);
}
