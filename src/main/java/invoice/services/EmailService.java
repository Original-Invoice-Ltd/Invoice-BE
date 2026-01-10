package invoice.services;

public interface EmailService {
    void sendWelcomeEmail(String to, String name);
    void sendVerificationEmail(String toEmail, String firstName, String verificationToken, String frontendUrl);
    void sendOTPEmail(String toEmail, String firstName, String otp);
    void sendPasswordResetOTPEmail(String toEmail, String firstName, String otp);
    void sendInvoiceNotificationEmail(String toEmail, String firstName, String invoiceId, String frontendUrl, String invoiceNumber, String invoiceDate, String dueDate, String amount, String clientName);
    
    void sendInvoiceNotificationEmail(String toEmail, String firstName, String invoiceId, String frontendUrl);
}
