package invoice.services;

import invoice.data.models.User;
import invoice.dtos.request.PasswordRequest;
import invoice.dtos.request.SignUpRequest;
import invoice.dtos.response.SignUpResponse;
import invoice.dtos.response.UserResponse;
import jakarta.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;

public interface UserService {
    void resendVerificationEmail(String email) throws MessagingException, UnsupportedEncodingException;

    User findByEmail(String username);
    UserResponse getProfileFor(String email);

    boolean isUserVerified(String email);

    boolean existsById(Long userId);
    boolean existsByEmail(String email);

    String deleteUser(Long id);

    String disableUser(Long id);

    String registerAdmin(SignUpRequest request);

    String deleteUserByEmail(String email);

    String uploadPhoto(String email, MultipartFile image);
    // Profile-related methods
    User getUserByEmail(String email);
    Long getUserIdByEmail(String email);

    String activate(String email);


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
