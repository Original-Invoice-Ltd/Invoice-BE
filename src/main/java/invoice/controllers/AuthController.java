package invoice.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import invoice.data.models.User;
import invoice.data.repositories.UserRepository;
import invoice.dtos.request.*;
import invoice.dtos.response.ErrorResponse;
import invoice.dtos.response.LoginResponse;
import invoice.dtos.response.OTPResponse;
import invoice.dtos.response.OriginalInvoiceApiResponse;
import invoice.exception.BusinessException;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.security.config.RsaKeyProperties;
import invoice.security.data.models.SecureUser;
import invoice.security.services.AuthService;
import invoice.services.UserService;
import invoice.utiils.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import static invoice.security.utils.SecurityUtils.JWT_PREFIX;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AuthService authService;
    private final RsaKeyProperties rsaKeys;

    private final  HttpServletResponse response;

    private final UserRepository userRepository;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "accessToken", required = false) String accessToken) {
        if (accessToken != null) {
            authService.blacklist(accessToken);
        }
        SecurityContextHolder.clearContext();
        
        // Clear cookies
        response.addCookie(CookieUtils.deleteAccessTokenCookie());
        response.addCookie(CookieUtils.deleteRefreshTokenCookie());
        
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        try {
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token not found"));
            }
            
            // Verify refresh token
            Algorithm algorithm = Algorithm.RSA512(rsaKeys.publicKey(), rsaKeys.privateKey());
            var verifier = JWT.require(algorithm)
                    .withIssuer("OriginalInvoiceRefreshToken")
                    .build();
            
            var decodedJWT = verifier.verify(refreshToken);
            String email = decodedJWT.getSubject();
            
            // Load user and create new authentication
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException("User not found"));
            
            // Create authentication object for token generation
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, user.getPassword())
            );
            
            // Generate new access token
            String newAccessToken = generateAccessToken(authentication);
            
            // Set new access token cookie
            response.addCookie(CookieUtils.createAccessTokenCookie(newAccessToken));
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Token refreshed successfully"
            ));
            
        } catch (Exception e) {
            log.error("Token refresh failed: ", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(()->new BusinessException("user not found"));
            
//            if(!user.isVerified()) {
//                ErrorResponse errorResponse = ErrorResponse.builder()
//                        .responseTime(now())
//                        .isSuccessful(false)
//                        .error("AccountNotVerified")
//                        .message("Account not verified. Please check your email and verify your account.")
//                        .path("/api/auth/login")
//                        .build();
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
//            }
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail().toLowerCase(),
                            loginRequest.getPassword()
                    )
            );

            SecureUser userDetails = (SecureUser) authentication.getPrincipal();
            
            // Generate access token (15 minutes)
            String accessToken = generateAccessToken(authentication);
            
            // Generate refresh token (30 days)
            String refreshToken = generateRefreshToken(authentication);

            // Set tokens as HTTP-only secure cookies
            response.addCookie(CookieUtils.createAccessTokenCookie(accessToken));
            response.addCookie(CookieUtils.createRefreshTokenCookie(refreshToken));

            // Return response WITHOUT tokens in body
            LoginResponse loginResponse = LoginResponse.builder()
                    .responseTime(now())
                    .message("Login successful")
                    .fullName(userDetails.getFullName())
                    .email(userDetails.getUsername())
                    .mediaUrl(userDetails.getMediaUrl())
                    .roles(userDetails.getRoles())
                    .build();

            return ResponseEntity.ok(new OriginalInvoiceApiResponse<>(true, loginResponse));
        } catch (BadCredentialsException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .responseTime(now())
                    .isSuccessful(false)
                    .error("UnsuccessfulAuthentication")
                    .message("Invalid credentials")
                    .path("/api/auth/login")
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (BusinessException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .responseTime(now())
                    .isSuccessful(false)
                    .error("BusinessException")
                    .message(e.getMessage())
                    .path("/api/auth/login")
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    private String generateAccessToken(Authentication authentication) {
        Algorithm algorithm = Algorithm.RSA512(rsaKeys.publicKey(), rsaKeys.privateKey());
        Instant now = Instant.now();
        UserDetails principal = (UserDetails) authentication.getPrincipal();

        return JWT.create()
                .withIssuer("OriginalInvoiceAccessToken")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(15, MINUTES))
                .withSubject(principal.getUsername())
                .withClaim("principal", principal.getUsername())
                .withClaim("credentials", authentication.getCredentials().toString())
                .withArrayClaim("roles", extractAuthorities(authentication.getAuthorities()))
                .withClaim("type", "access")
                .sign(algorithm);
    }
    
    private String generateRefreshToken(Authentication authentication) {
        Algorithm algorithm = Algorithm.RSA512(rsaKeys.publicKey(), rsaKeys.privateKey());
        Instant now = Instant.now();
        UserDetails principal = (UserDetails) authentication.getPrincipal();

        return JWT.create()
                .withIssuer("OriginalInvoiceRefreshToken")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(30, java.time.temporal.ChronoUnit.DAYS))
                .withSubject(principal.getUsername())
                .withClaim("principal", principal.getUsername())
                .withClaim("type", "refresh")
                .sign(algorithm);
    }

    private String[] extractAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toArray(String[]::new);
    }


    @PutMapping("/change-password")
    public ResponseEntity<?>changePassword(@RequestBody PasswordRequest passwordRequest) {
        try {
            String response = userService.resetPassword(passwordRequest);
            return ResponseEntity.ok(response);
        }
        catch (OriginalInvoiceBaseException e){
            return new ResponseEntity<>(e.getMessage(), BAD_REQUEST);
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestParam String token) {
        try {
            boolean isVerified = userService.verifyUser(token);
            if (isVerified) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email verified successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid or expired verification token"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(BAD_REQUEST).body(Map.of(
                "success", false,
                "message", "Verification failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@Valid @RequestBody OTPVerificationRequest request) {
        try {
            boolean isVerified = userService.verifyUserWithOTP(request.getEmail(), request.getOtp());
            
            OTPResponse response = OTPResponse.builder()
                    .success(isVerified)
                    .message(isVerified ? "Email verified successfully" : "Invalid OTP")
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.ok(new OriginalInvoiceApiResponse<>(true, response));
        } catch (BusinessException e) {
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.badRequest().body(new OriginalInvoiceApiResponse<>(false, response));
        } catch (OriginalInvoiceBaseException e) {
            log.error("OTP verification error: ", e);
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message("Verification failed: " + e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new OriginalInvoiceApiResponse<>(false, response));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOTP(@Valid @RequestBody ResendOTPRequest request) {
        try {
            boolean sent = userService.resendOTP(request.getEmail());
            
            OTPResponse response = OTPResponse.builder()
                    .success(sent)
                    .message(sent ? "OTP sent successfully" : "Failed to send OTP")
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.ok(new OriginalInvoiceApiResponse<>(true, response));
        } catch (BusinessException e) {
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.badRequest().body(new OriginalInvoiceApiResponse<>(false, response));
        } catch (Exception e) {
            log.error("Resend OTP error: ", e);
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message("Failed to resend OTP: " + e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OriginalInvoiceApiResponse<>(false, response));
        }
    }

    @PostMapping("/send-verification-otp")
    public ResponseEntity<?> sendVerificationOTP(@Valid @RequestBody ResendOTPRequest request) {
        try {
            userService.sendVerificationOTP(request.getEmail());
            
            OTPResponse response = OTPResponse.builder()
                    .success(true)
                    .message("Verification OTP sent successfully")
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.ok(new OriginalInvoiceApiResponse<>(true, response));
        } catch (BusinessException e) {
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.badRequest().body(new OriginalInvoiceApiResponse<>(false, response));
        } catch (Exception e) {
            log.error("Send verification OTP error: ", e);
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message("Failed to send verification OTP: " + e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OriginalInvoiceApiResponse<>(false, response));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            boolean sent = userService.sendPasswordResetOTP(request.getEmail());
            
            OTPResponse response = OTPResponse.builder()
                    .success(sent)
                    .message(sent ? "Password reset code sent successfully" : "Failed to send password reset code")
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.ok(new OriginalInvoiceApiResponse<>(true, response));
        } catch (BusinessException e) {
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.badRequest().body(new OriginalInvoiceApiResponse<>(false, response));
        } catch (Exception e) {
            log.error("Forgot password error: ", e);
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message("Failed to send password reset code: " + e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new OriginalInvoiceApiResponse<>(false, response));
        }
    }

    @PostMapping("/verify-password-reset-otp")
    public ResponseEntity<?> verifyPasswordResetOTP(@Valid @RequestBody OTPVerificationRequest request) {
        try {
            boolean isVerified = userService.verifyPasswordResetOTP(request.getEmail(), request.getOtp());
            
            OTPResponse response = OTPResponse.builder()
                    .success(isVerified)
                    .message(isVerified ? "OTP verified successfully" : "Invalid OTP")
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.ok(new OriginalInvoiceApiResponse<>(true, response));
        } catch (BusinessException e) {
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.badRequest().body(new OriginalInvoiceApiResponse<>(false, response));
        } catch (Exception e) {
            log.error("Verify password reset OTP error: ", e);
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message("Verification failed: " + e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new OriginalInvoiceApiResponse<>(false, response));
        }
    }

    @PostMapping("/reset-password-with-otp")
    public ResponseEntity<?> resetPasswordWithOTP(@Valid @RequestBody ForgotPasswordOTPRequest request) {
        try {
            boolean isReset = userService.resetPasswordWithOTP(request.getEmail(), request.getOtp(), request.getNewPassword());
            
            OTPResponse response = OTPResponse.builder()
                    .success(isReset)
                    .message(isReset ? "Password reset successfully" : "Failed to reset password")
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.ok(new OriginalInvoiceApiResponse<>(true, response));
        } catch (BusinessException e) {
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.badRequest().body(new OriginalInvoiceApiResponse<>(false, response));
        } catch (Exception e) {
            log.error("Reset password with OTP error: ", e);
            OTPResponse response = OTPResponse.builder()
                    .success(false)
                    .message("Failed to reset password: " + e.getMessage())
                    .responseTime(now())
                    .build();
            
            return ResponseEntity.status(BAD_REQUEST)
                    .body(new OriginalInvoiceApiResponse<>(false, response));
        }
    }

}