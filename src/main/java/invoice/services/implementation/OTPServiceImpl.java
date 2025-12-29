package invoice.services.implementation;


import invoice.data.constants.UserStatus;
import invoice.data.models.User;
import invoice.data.models.VerificationToken;
import invoice.data.repositories.UserRepository;
import invoice.data.repositories.VerificationTokenRepository;
import invoice.exception.BusinessException;
import invoice.services.EmailService;
import invoice.services.OTPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPServiceImpl implements OTPService {
    
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOTP() {
        // Generate 6-digit OTP
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }


    @Override
    @Transactional
    public VerificationToken createOTPToken(User user) {
        log.info("Creating OTP token for user: id={}, email={}", user.getId(), user.getEmail());
        
        // Try to find existing token and update it instead of creating new one
        Optional<VerificationToken> existingTokenOpt = tokenRepository.findByUser(user);
        
        if (existingTokenOpt.isPresent()) {
            log.info("Found existing token for user: {}, updating it", user.getEmail());
            VerificationToken existingToken = existingTokenOpt.get();
            
            // Update the existing token with new OTP and reset expiry
            existingToken.setOtp(generateOTP());
            existingToken.setOtpExpiryDate(LocalDateTime.now().plusMinutes(10));
            existingToken.resetOtpAttempts();
            existingToken.setVerificationType(VerificationToken.VerificationType.OTP);
            
            try {
                VerificationToken savedToken = tokenRepository.save(existingToken);
                log.info("Successfully updated existing OTP token for user: {}", user.getEmail());
                return savedToken;
            } catch (Exception e) {
                log.error("Failed to update existing OTP token for user {}: {}", user.getEmail(), e.getMessage(), e);
                throw new BusinessException("Failed to update verification token: " + e.getMessage());
            }
        } else {
            log.info("No existing token found for user: {}, creating new one", user.getEmail());
            
            // Create new OTP token
            VerificationToken token = new VerificationToken(user, VerificationToken.VerificationType.OTP);
            token.setOtp(generateOTP());
            
            try {
                VerificationToken savedToken = tokenRepository.save(token);
                log.info("Successfully created new OTP token for user: {}", user.getEmail());
                return savedToken;
            } catch (Exception e) {
                log.error("Failed to create new OTP token for user {}: {}", user.getEmail(), e.getMessage(), e);
                
                // If we get a constraint violation, it means a token was created between our check and save
                // Try to find and update the existing token
                try {
                    log.info("Attempting to find and update token that was created concurrently for user: {}", user.getEmail());
                    Optional<VerificationToken> concurrentToken = tokenRepository.findByUser(user);
                    if (concurrentToken.isPresent()) {
                        VerificationToken existingToken = concurrentToken.get();
                        existingToken.setOtp(generateOTP());
                        existingToken.setOtpExpiryDate(LocalDateTime.now().plusMinutes(10));
                        existingToken.resetOtpAttempts();
                        existingToken.setVerificationType(VerificationToken.VerificationType.OTP);
                        
                        VerificationToken savedToken = tokenRepository.save(existingToken);
                        log.info("Successfully updated concurrent token for user: {}", user.getEmail());
                        return savedToken;
                    }
                } catch (Exception e2) {
                    log.error("Failed to handle concurrent token creation for user {}: {}", user.getEmail(), e2.getMessage(), e2);
                }
                
                throw new BusinessException("Failed to create verification token: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean validateOTP(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        Optional<VerificationToken> tokenOpt = tokenRepository.findByUser(user);
        if (tokenOpt.isEmpty()) {
            throw new BusinessException("No verification token found. Please request a new OTP.");
        }
        
        VerificationToken token = tokenOpt.get();
        
        // Check if OTP has expired
        if (token.isOtpExpired())
            throw new BusinessException("OTP has expired. Please request a new one.");

        
        // Check if max attempts exceeded
        if (token.hasExceededMaxAttempts()) {
            tokenRepository.delete(token);
            throw new BusinessException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }
        
        // Validate OTP
        if (!otp.equals(token.getOtp())) {
            token.incrementAttempts();
            tokenRepository.save(token);
            
            int remainingAttempts = token.getMaxAttempts() - token.getOtpAttempts();
            throw new BusinessException("Invalid OTP. " + remainingAttempts + " attempts remaining.");
        }
        
        // OTP is valid - verify user and cleanup
        user.setVerified(true);
        user.setStatus(UserStatus.VERIFIED);
        userRepository.save(user);
        
        // Delete the token after successful verification
        tokenRepository.delete(token);
        
        // Send welcome email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
        
        return true;
    }

    @Override
    @Transactional
    public boolean resendOTP(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        if (user.isVerified()) {
            throw new BusinessException("This account has already been verified.");
        }
        
        // Create new OTP token (this will delete the existing one)
        VerificationToken newToken = createOTPToken(user);
        
        // Send OTP email
        try {
            emailService.sendOTPEmail(user.getEmail(), user.getEmail(), newToken.getOtp());
            return true;
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", user.getEmail(), e.getMessage());
            throw new BusinessException("Failed to send OTP email: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public VerificationToken createPasswordResetOTPToken(User user) {
        // Try to find existing token and update it instead of creating new one
        Optional<VerificationToken> existingTokenOpt = tokenRepository.findByUser(user);
        
        if (existingTokenOpt.isPresent()) {
            VerificationToken existingToken = existingTokenOpt.get();
            // Update the existing token with new OTP and reset expiry
            existingToken.setOtp(generateOTP());
            existingToken.setOtpExpiryDate(LocalDateTime.now().plusMinutes(10));
            existingToken.resetOtpAttempts();
            existingToken.setVerificationType(VerificationToken.VerificationType.OTP);
            try {
                return tokenRepository.save(existingToken);
            } catch (Exception e) {
                log.error("Failed to update existing token for password reset for user {}: {}", user.getEmail(), e.getMessage(), e);
                throw new BusinessException("Failed to update password reset token: " + e.getMessage());
            }
        } else {
            log.info("No existing token found for user: {}, creating new password reset token", user.getEmail());
            
            // Create new password reset OTP token (using OTP type temporarily until migration runs)
            VerificationToken token = new VerificationToken(user, VerificationToken.VerificationType.OTP);
            token.setOtp(generateOTP());
            
            try {
                VerificationToken savedToken = tokenRepository.save(token);
                log.info("Successfully created new password reset OTP token for user: {}", user.getEmail());
                return savedToken;
            } catch (Exception e) {
                log.error("Failed to create new password reset OTP token for user {}: {}", user.getEmail(), e.getMessage(), e);
                
                // If we get a constraint violation, it means a token was created between our check and save
                // Try to find and update the existing token
                try {
                    log.info("Attempting to find and update token that was created concurrently for password reset for user: {}", user.getEmail());
                    Optional<VerificationToken> concurrentToken = tokenRepository.findByUser(user);
                    if (concurrentToken.isPresent()) {
                        VerificationToken existingToken = concurrentToken.get();
                        existingToken.setOtp(generateOTP());
                        existingToken.setOtpExpiryDate(LocalDateTime.now().plusMinutes(10));
                        existingToken.resetOtpAttempts();
                        existingToken.setVerificationType(VerificationToken.VerificationType.OTP);
                        
                        VerificationToken savedToken = tokenRepository.save(existingToken);
                        log.info("Successfully updated concurrent token for password reset for user: {}", user.getEmail());
                        return savedToken;
                    }
                } catch (Exception e2) {
                    log.error("Failed to handle concurrent token creation for password reset for user {}: {}", user.getEmail(), e2.getMessage(), e2);
                }
                
                throw new BusinessException("Failed to create password reset token: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean validatePasswordResetOTP(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        Optional<VerificationToken> tokenOpt = tokenRepository.findByUser(user);
        if (tokenOpt.isEmpty()) {
            throw new BusinessException("No password reset token found. Please request a new OTP.");
        }
        
        VerificationToken token = tokenOpt.get();
        
        // Check if this is a password reset token (temporarily accepting OTP type until migration runs)
        if (token.getVerificationType() != VerificationToken.VerificationType.OTP && 
            token.getVerificationType() != VerificationToken.VerificationType.PASSWORD_RESET) {
            throw new BusinessException("Invalid token type. Please request a password reset.");
        }
        
        // Check if OTP has expired
        if (token.isOtpExpired()) {
            throw new BusinessException("OTP has expired. Please request a new one.");
        }
        
        // Check if max attempts exceeded
        if (token.hasExceededMaxAttempts()) {
            tokenRepository.delete(token);
            throw new BusinessException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }
        
        // Validate OTP
        if (!otp.equals(token.getOtp())) {
            token.incrementAttempts();
            tokenRepository.save(token);
            
            int remainingAttempts = token.getMaxAttempts() - token.getOtpAttempts();
            throw new BusinessException("Invalid OTP. " + remainingAttempts + " attempts remaining.");
        }
        
        // OTP is valid - don't delete token yet, it will be used for password reset
        return true;
    }

    @Override
    public boolean resendPasswordResetOTP(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        // Create new password reset OTP token (this will delete the existing one)
        VerificationToken newToken = createPasswordResetOTPToken(user);
        
        // Send password reset OTP email
        try {
            emailService.sendPasswordResetOTPEmail(user.getEmail(), user.getEmail(), newToken.getOtp());
            return true;
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to {}: {}", user.getEmail(), e.getMessage());
            throw new BusinessException("Failed to send password reset OTP email: " + e.getMessage());
        }
    }
}