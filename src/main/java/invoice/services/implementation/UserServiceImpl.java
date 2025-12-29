package invoice.services.implementation;

import com.cloudinary.Cloudinary;
import invoice.data.constants.Role;
import invoice.data.models.User;
import invoice.data.models.VerificationToken;
import invoice.data.repositories.UserRepository;
import invoice.data.repositories.VerificationTokenRepository;
import invoice.dtos.request.PasswordRequest;
import invoice.dtos.request.SignUpRequest;
import invoice.dtos.response.SignUpResponse;
import invoice.dtos.response.UserResponse;
import invoice.exception.BusinessException;
import invoice.exception.ResourceNotFoundException;
import invoice.services.EmailService;
import invoice.services.OTPService;
import invoice.services.UserService;
import jakarta.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static invoice.data.constants.Role.ADMIN;
import static invoice.data.constants.UserStatus.*;
import static invoice.utiils.ServiceUtils.getMediaUrl;

@Service
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final OTPService otpService;
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
        user.setStatus(VERIFIED);
        userRepository.save(user);
        tokenRepository.delete(vToken);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
        return true;
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) throws MessagingException, UnsupportedEncodingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (user.getStatus() == VERIFIED) {
            throw new BusinessException("This account has already been verified.");
        }

        // Delete existing token if any
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        // Create new verification token
        VerificationToken newToken = new VerificationToken(user);
        tokenRepository.save(newToken);
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), newToken.getToken(), "https://marketplace.bdic.ng/register/emailVerification");
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
        log.info("Attempting to send verification OTP for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        log.info("Found user: id={}, email={}", user.getId(), user.getEmail());

        if (user.getStatus() == VERIFIED) {
            throw new BusinessException("This account has already been verified.");
        }

        // Create OTP token and send OTP email
        try {
            VerificationToken verificationToken = otpService.createOTPToken(user);
            log.info("Created OTP token for user: {}", user.getEmail());

            emailService.sendOTPEmail(user.getEmail(), user.getFullName(), verificationToken.getOtp());
            log.info("Successfully sent verification OTP to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification OTP to {}: {}", user.getEmail(), e.getMessage(), e);
            throw new BusinessException("Failed to send verification OTP: " + e.getMessage());
        }
    }

    @Override
    public User findByEmail(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    @Override
    public UserResponse getProfileFor(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return new UserResponse(user);
    }


    @Override
    public boolean isUserVerified(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getStatus() == VERIFIED;
    }

    @Override
    public String resetPassword(PasswordRequest passwordRequest) {
        User user = userRepository.findByEmail(passwordRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(passwordRequest.getOldPassword(), user.getPassword())) {
            throw new BusinessException("Passwords do not match");
        }
        user.setPassword(passwordEncoder.encode(passwordRequest.getNewPassword()));
        userRepository.save(user);
        return "Password reset successful";
    }

    @Override
    public boolean existsById(UUID userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public String deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);
        userRepository.delete(user);

        return "user deleted";
    }

    @Override
    public String disableUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setStatus(INACTIVE);
        userRepository.save(user);
        return "user deleted";
    }

    @Override
    public String registerAdmin(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UsernameNotFoundException("user exists with email");
        }

        User.UserBuilder userBuilder = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .status(VERIFIED)
                .password(passwordEncoder.encode(request.getPassword()));
        User user = userBuilder.build();
        user.setRoles(new HashSet<>());
        user.getRoles().add(ADMIN);
        userRepository.save(user);
        return "User registered successfully.";
    }


    @Override
    public String deleteUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);
        userRepository.delete(user);
        return "user deleted";
    }

    @Override
    public String uploadPhoto(String email, MultipartFile image) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String imageUrl = getMediaUrl(image, cloudinary.uploader());
        user.setMediaUrl(imageUrl);
        userRepository.save(user);
        return "photo updated successfully";
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public UUID getUserIdByEmail(String email) {
        User user = getUserByEmail(email);
        return user.getId();
    }

    @Override
    public String activate(String email) {
        User user = getUserByEmail(email);
        user.setStatus(VERIFIED);
        userRepository.save(user);
        return "user activated";
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
        VerificationToken verificationToken = otpService.createOTPToken(savedUser);
        emailService.sendOTPEmail(savedUser.getEmail(), savedUser.getFullName(), verificationToken.getOtp());
        SignUpResponse response = new SignUpResponse();
        response.setMessage("User registered successfully.");
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }


}
