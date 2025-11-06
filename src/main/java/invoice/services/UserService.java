package invoice.services;

import invoice.dtos.request.PasswordRequest;
import jakarta.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;

public interface UserService {
    boolean verifyUser(String token);
    boolean verifyUserWithOTP(String email, String otp);
    boolean resendOTP(String email);
    void sendVerificationOTP(String email);
    String resetPassword(PasswordRequest passwordRequest);
    boolean sendPasswordResetOTP(String email);
    boolean verifyPasswordResetOTP(String email, String otp);
    boolean resetPasswordWithOTP(String email, String otp, String newPassword);
}
