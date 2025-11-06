package invoice.services;

public interface EmailService {
    void sendWelcomeEmail(String to, String name);
    void sendVerificationEmail(String toEmail, String firstName, String verificationToken, String frontendUrl);
    void sendOTPEmail(String toEmail, String firstName, String otp);
    void sendPasswordResetOTPEmail(String toEmail, String firstName, String otp);
}
