package invoice.services;


import invoice.data.models.User;
import invoice.data.models.VerificationToken;

public interface OTPService {
    String generateOTP();
    VerificationToken createOTPToken(User user);
    boolean validateOTP(String email, String otp);
    boolean resendOTP(String email);
    VerificationToken createPasswordResetOTPToken(User user);
    boolean validatePasswordResetOTP(String email, String otp);
    boolean resendPasswordResetOTP(String email);
}