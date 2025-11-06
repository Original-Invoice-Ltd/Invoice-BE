package invoice.data.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens")
@Setter
@Getter
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String token;

    @Column(length = 4)
    private String otp;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private LocalDateTime expiryDate;
    
    private LocalDateTime otpExpiryDate;

    @Enumerated(EnumType.STRING)
    private VerificationType verificationType = VerificationType.OTP;

    @Column(name = "otp_attempts")
    private Integer otpAttempts = 0;

    @Column(name = "max_attempts")
    private Integer maxAttempts = 5;

    // constructors, getters and setters
    public VerificationToken(User user) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
        this.expiryDate = LocalDateTime.now().plusHours(24); // 24 hour expiry for token
        this.otpExpiryDate = LocalDateTime.now().plusMinutes(10); // 10 minute expiry for OTP
        this.verificationType = VerificationType.OTP;
        this.otpAttempts = 0;
        this.maxAttempts = 5; // Set default max attempts
    }

    public VerificationToken(User user, VerificationType type) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
        this.verificationType = type;
        
        if (type == VerificationType.OTP || type == VerificationType.PASSWORD_RESET) {
            this.expiryDate = LocalDateTime.now().plusHours(24);
            this.otpExpiryDate = LocalDateTime.now().plusMinutes(10);
            this.otpAttempts = 0;
            this.maxAttempts = 5; // Set default max attempts
        } else {
            this.expiryDate = LocalDateTime.now().plusHours(24);
            this.otpAttempts = 0;
            this.maxAttempts = 5; // Set default max attempts for all types
        }
    }

    public VerificationToken() {
        this.otpAttempts = 0;
        this.maxAttempts = 5; // Set default max attempts in default constructor
    }

    public boolean isOtpExpired() {
        return otpExpiryDate != null && otpExpiryDate.isBefore(LocalDateTime.now());
    }

    public boolean hasExceededMaxAttempts() {
        int attempts = (otpAttempts != null) ? otpAttempts : 0;
        int maxAtts = (maxAttempts != null) ? maxAttempts : 5;
        return attempts >= maxAtts;
    }

    public void incrementAttempts() {
        this.otpAttempts = (otpAttempts != null) ? otpAttempts + 1 : 1;
    }

    public void resetOtpAttempts() {
        this.otpAttempts = 0;
        this.otpExpiryDate = LocalDateTime.now().plusMinutes(10);
    }
    
    public int getOtpAttempts() {
        return (otpAttempts != null) ? otpAttempts : 0;
    }
    
    public int getMaxAttempts() {
        return (maxAttempts != null) ? maxAttempts : 5;
    }
    
    @PostLoad
    private void initializeDefaults() {
        if (otpAttempts == null) {
            otpAttempts = 0;
        }
        if (maxAttempts == null) {
            maxAttempts = 5;
        }
    }

    public enum VerificationType {
        EMAIL_LINK, OTP, PASSWORD_RESET
    }
}