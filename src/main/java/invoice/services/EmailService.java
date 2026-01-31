package invoice.services;

public interface EmailService {
    void sendWelcomeEmail(String to, String name);
    void sendVerificationEmail(String toEmail, String firstName, String verificationToken, String frontendUrl);
    void sendOTPEmail(String toEmail, String firstName, String otp);
    void sendPasswordResetOTPEmail(String toEmail, String firstName, String otp);
    void sendInvoiceNotificationEmail(String toEmail, String firstName, String invoiceId, String frontendUrl, String invoiceNumber, String invoiceDate, String dueDate, String amount, String clientName);
    void sendPaymentEvidenceNotificationEmail(String toEmail, String senderName, String invoiceNumber, String customerName, String dashboardUrl);
    void sendPaymentReceiptEmail(String toEmail, String customerName, String receiptNumber, String receiptDate, String invoiceNumber, String invoiceIssueDate, String itemsJson, String subtotal, String vat, String totalAmount, String paymentMethod, String paymentDate, String confirmedBy);
    
    void sendInvoiceNotificationEmail(String toEmail, String firstName, String invoiceId, String frontendUrl);
}
