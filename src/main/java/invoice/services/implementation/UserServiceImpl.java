package invoice.services.implementation;

import com.cloudinary.Cloudinary;
import invoice.data.constants.Role;
import invoice.data.constants.UserStatus;
import invoice.data.models.User;
import invoice.data.models.VerificationToken;
import invoice.data.repositories.UserRepository;
import invoice.data.repositories.VerificationTokenRepository;
import invoice.dtos.request.PasswordRequest;
import invoice.dtos.request.SignUpRequest;
import invoice.dtos.response.SignUpResponse;
import invoice.exception.BusinessException;
import invoice.exception.ResourceNotFoundException;
import invoice.services.EmailService;
import invoice.services.OTPService;
import invoice.services.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static invoice.data.constants.UserStatus.PENDING;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final OTPService otpService;
    private final ModelMapper modelMapper;
    private final VerificationTokenRepository tokenRepository;
    private final Cloudinary cloudinary;

    @Override
    public boolean verifyUser(String token) {
        Optional<VerificationToken> verificationToken = tokenRepository.findByToken(token);
        if (verificationToken.isEmpty()) {
            return false;
        }
        VerificationToken vToken = verificationToken.get();
        if (vToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }
        User user = vToken.getUser();
        user.setStatus(UserStatus.VERIFIED);
        user.setVerified(true);
        userRepository.save(user);
        tokenRepository.delete(vToken);
        emailService.sendWelcomeEmail(user.getEmail(),user.getEmail());
        return true;
    }

    @Override
    public boolean verifyUserWithOTP(String email, String otp) {
        return otpService.validateOTP(email, otp);
    }

    @Override
    public boolean resendOTP(String email) {
        return otpService.resendOTP(email);
    }

    @Override
    @Transactional
    public void sendVerificationOTP(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (user.isVerified())
            throw new BusinessException("This account has already been verified.");

        // Create OTP token and send OTP email
        try {
            VerificationToken verificationToken = otpService.createOTPToken(user);

            emailService.sendOTPEmail(user.getEmail(), user.getEmail(), verificationToken.getOtp());
        } catch (Exception e) {
            log.error("Failed to send verification OTP to {}: {}", user.getEmail(), e.getMessage(), e);
            throw new BusinessException("Failed to send verification OTP: " + e.getMessage());
        }
    }

    @Override
    public String resetPassword(PasswordRequest passwordRequest) {
        User user = userRepository.findByEmail(passwordRequest.getEmail())
                .orElseThrow(() -> new BusinessException("User not found"));
        if(!passwordEncoder.matches(passwordRequest.getOldPassword(), user.getPassword())) {
            throw new BusinessException("Passwords do not match");
        }
        user.setPassword(passwordEncoder.encode(passwordRequest.getNewPassword()));
        userRepository.save(user);
        return "Password reset successful";
    }

    @Override
    public boolean sendPasswordResetOTP(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.isVerified()) {
            throw new BusinessException("Account not verified. Please verify your account first.");
        }

        return otpService.resendPasswordResetOTP(email);
    }

    @Override
    public boolean verifyPasswordResetOTP(String email, String otp) {
        return otpService.validatePasswordResetOTP(email, otp);
    }

    @Override
    public boolean resetPasswordWithOTP(String email, String otp, String newPassword) {
        // First verify the OTP
        if (!otpService.validatePasswordResetOTP(email, otp)) {
            return false;
        }
        // Update the password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // Clean up the token after successful password reset
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        return true;
    }

    @Override
    public SignUpResponse register(SignUpRequest signUpRequest) {
        boolean existsByEmail = userRepository.existsByEmail(signUpRequest.getEmail());
        if (existsByEmail) throw new BusinessException("user exists with email");

        User user = User.builder()
                .email(signUpRequest.getEmail())
                .status(PENDING)
                .fullName(signUpRequest.getFullName())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .build();
        user.setRoles(new HashSet<>());
        user.getRoles().add(Role.USER);
        User savedUser = userRepository.save(user);

        // Create OTP token and send OTP email
//        VerificationToken verificationToken = otpService.createOTPToken(savedUser);
//        emailService.sendOTPEmail(savedUser.getEmail(), savedUser.getFullName(), verificationToken.getOtp());
        SignUpResponse response = new SignUpResponse();
        response.setMessage("User registered successfully.");
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }


}
