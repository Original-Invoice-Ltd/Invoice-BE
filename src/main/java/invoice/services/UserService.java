package invoice.services;

import invoice.dtos.request.PasswordRequest;
import invoice.dtos.request.SignUpRequest;
import invoice.dtos.response.SignUpResponse;

public interface UserService {
    boolean verifyUser(String token);
    boolean verifyUserWithOTP(String email, String otp);
    boolean resendOTP(String email);
    void sendVerificationOTP(String email);
    String resetPassword(PasswordRequest passwordRequest);
    boolean sendPasswordResetOTP(String email);
    boolean verifyPasswordResetOTP(String email, String otp);
    boolean resetPasswordWithOTP(String email, String otp, String newPassword);

    SignUpResponse register(SignUpRequest signUpRequest);
}
