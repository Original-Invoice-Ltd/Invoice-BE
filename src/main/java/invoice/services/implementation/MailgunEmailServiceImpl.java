package invoice.services.implementation;

import invoice.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@Slf4j
public class MailgunEmailServiceImpl implements EmailService {

    @Value("${mailgun.api.key}")
    private String apiKey;

    @Value("${mailgun.domain}")
    private String domain;

    @Value("${mailgun.from.name}")
    private String fromName;

    @Value("${mailgun.from.email}")
    private String fromEmail;

    private final RestTemplate restTemplate;

    public MailgunEmailServiceImpl() {
        this.restTemplate = new RestTemplate();
    }


    @Override
    public void sendWelcomeEmail(String toEmail, String name) {
        String subject = "Welcome to Agro Smart Benue!";
        String htmlContent = buildWelcomeEmailBody(name);

        try {
            sendEmailInternal(name, toEmail, subject, htmlContent);
            log.info("Welcome email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    @Override
    public void sendVerificationEmail(String toEmail, String firstName, String verificationToken, String frontendUrl) {
        String subject = "Verify your email address";
        String verificationUrl = frontendUrl + "?token=" + verificationToken;
        String htmlContent = buildVerificationEmailTemplate(firstName, verificationUrl);

        try {
            sendEmailInternal(firstName, toEmail, subject, htmlContent);
            log.info("Verification email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendOTPEmail(String toEmail, String firstName, String otp) {
        String subject = "Your Verification Code";
        String htmlContent = buildOTPEmailTemplate(firstName, otp);

        try {
            sendEmailInternal(firstName, toEmail, subject, htmlContent);
            log.info("OTP email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    @Override
    public void sendPasswordResetOTPEmail(String toEmail, String firstName, String otp) {
        String subject = "Password Reset Code";
        String htmlContent = buildPasswordResetOTPEmailTemplate(firstName, otp);

        try {
            sendEmailInternal(firstName, toEmail, subject, htmlContent);
            log.info("Password reset email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    // ================== Helper Method ==================

    private void sendEmailInternal(String toName, String toEmail, String subject, String htmlContent) {
        String url = String.format("https://api.mailgun.net/v3/%s/messages", domain);

        // Create headers with Basic Auth
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = "api:" + apiKey;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);

        // Create form data
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("from", String.format("%s <%s>", fromName, fromEmail));
        body.add("to", String.format("%s <%s>", toName, toEmail));
        body.add("subject", subject);
        body.add("html", htmlContent);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Email sent successfully to {} ({})", toName, toEmail);
            } else {
                log.error("Failed to send email. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send email via Mailgun");
            }
        } catch (Exception e) {
            log.error("Error sending email via Mailgun: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email via Mailgun", e);
        }
    }

    // ================== HTML Templates ==================

    private String buildVerificationEmailTemplate(String firstName, String verificationUrl) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #f5f7fa 0%%, #c3cfe2 100%%); padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 40px rgba(0,0,0,0.1);">
                <tr>
                    <td style="background: linear-gradient(135deg, #16a34a 0%%, #15803d 100%%); padding: 40px 30px; text-align: center; position: relative;">
                        <h1 style="color: white; margin: 0; font-size: 32px; font-weight: 700; text-shadow: 0 2px 4px rgba(0,0,0,0.2);">Agro Smart Benue</h1>
                        <p style="color: rgba(255,255,255,0.9); margin: 12px 0 0 0; font-size: 15px;">Empowering Farmers, Growing Communities</p>
                    </td>
                </tr>
                <tr>
                    <td style="padding: 40px 35px;">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <div style="display: inline-block; background: linear-gradient(135deg, #dcfce7 0%%, #bbf7d0 100%%); padding: 15px; border-radius: 50%%; margin-bottom: 20px;">
                                <span style="font-size: 40px;">✉️</span>
                            </div>
                            <h2 style="color: #16a34a; margin: 0 0 10px 0; font-size: 26px; font-weight: 700;">Welcome Aboard!</h2>
                            <p style="color: #6b7280; font-size: 15px; margin: 0;">We're excited to have you join our community</p>
                        </div>
                        
                        <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">Hi <strong style="color: #16a34a;">%s</strong>,</p>
                        <p style="color: #4b5563; font-size: 15px; line-height: 1.7; margin: 0 0 30px 0;">Thank you for registering with Agro Smart Benue! You're just one step away from accessing our platform. Please verify your email address to complete your registration.</p>
                        
                        <div style="text-align: center; margin: 35px 0;">
                            <a href="%s" style="background: linear-gradient(135deg, #16a34a 0%%, #15803d 100%%); color: white; padding: 16px 40px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px; box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3); transition: all 0.3s;">
                                ✓ Verify Email Address
                            </a>
                        </div>
                        
                        <div style="background: linear-gradient(135deg, #fef3c7 0%%, #fde68a 100%%); border-left: 4px solid #f59e0b; padding: 15px 20px; border-radius: 8px; margin: 30px 0;">
                            <p style="color: #92400e; font-size: 14px; margin: 0; line-height: 1.6;">
                                <strong>⏰ Important:</strong> This verification link will expire in 24 hours.
                            </p>
                        </div>
                        
                        <p style="color: #6b7280; font-size: 14px; line-height: 1.6; margin: 20px 0 0 0;">If you didn't create an account, please ignore this email and no action will be taken.</p>
                    </td>
                </tr>
                <tr>
                    <td style="background: linear-gradient(135deg, #f9fafb 0%%, #f3f4f6 100%%); padding: 30px 35px; border-top: 1px solid #e5e7eb;">
                        <p style="color: #9ca3af; font-size: 13px; text-align: center; margin: 0 0 10px 0; line-height: 1.6;">
                            Best regards,<br>
                            <strong style="color: #16a34a;">The Agro Smart Benue Team</strong>
                        </p>
                        <p style="color: #d1d5db; font-size: 11px; text-align: center; margin: 15px 0 0 0;">
                            © 2024 Agro Smart Benue. All rights reserved.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(firstName, verificationUrl);
    }

    private String buildOTPEmailTemplate(String firstName, String otp) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #f5f7fa 0%%, #c3cfe2 100%%); padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 40px rgba(0,0,0,0.1);">
                <tr>
                    <td style="background: linear-gradient(135deg, #16a34a 0%%, #15803d 100%%); padding: 40px 30px; text-align: center; position: relative;">
                        <h1 style="color: white; margin: 0; font-size: 32px; font-weight: 700; text-shadow: 0 2px 4px rgba(0,0,0,0.2);">Agro Smart Benue</h1>
                        <p style="color: rgba(255,255,255,0.9); margin: 12px 0 0 0; font-size: 15px;">Empowering Farmers, Growing Communities</p>
                    </td>
                </tr>
                <tr>
                    <td style="padding: 40px 35px;">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <div style="display: inline-block; background: linear-gradient(135deg, #dcfce7 0%%, #bbf7d0 100%%); padding: 15px; border-radius: 50%%; margin-bottom: 20px;">
                                <span style="font-size: 40px;">🔐</span>
                            </div>
                            <h2 style="color: #16a34a; margin: 0 0 10px 0; font-size: 26px; font-weight: 700;">Verification Code</h2>
                            <p style="color: #6b7280; font-size: 15px; margin: 0;">Enter this code to verify your email</p>
                        </div>
                        
                        <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">Hi <strong style="color: #16a34a;">%s</strong>,</p>
                        <p style="color: #4b5563; font-size: 15px; line-height: 1.7; margin: 0 0 30px 0;">Use the verification code below to complete your registration. This code is unique to you and should not be shared with anyone.</p>
                        
                        <div style="background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%); padding: 30px; text-align: center; margin: 30px 0; border-radius: 12px; border: 3px dashed #16a34a; box-shadow: 0 4px 12px rgba(22, 163, 74, 0.1);">
                            <p style="color: #15803d; font-size: 14px; font-weight: 600; margin: 0 0 15px 0; text-transform: uppercase; letter-spacing: 1px;">Your Verification Code</p>
                            <h1 style="color: #16a34a; font-size: 48px; letter-spacing: 8px; margin: 0; font-weight: 800; text-shadow: 0 2px 4px rgba(22, 163, 74, 0.1);">%s</h1>
                        </div>
                        
                        <div style="background: linear-gradient(135deg, #fef3c7 0%%, #fde68a 100%%); border-left: 4px solid #f59e0b; padding: 15px 20px; border-radius: 8px; margin: 30px 0;">
                            <p style="color: #92400e; font-size: 14px; margin: 0; line-height: 1.6;">
                                <strong>⏰ Expires in 10 minutes</strong><br>
                                Please enter this code promptly to complete your verification.
                            </p>
                        </div>
                        
                        <div style="background: linear-gradient(135deg, #fee2e2 0%%, #fecaca 100%%); border-left: 4px solid #dc2626; padding: 15px 20px; border-radius: 8px; margin: 20px 0 0 0;">
                            <p style="color: #991b1b; font-size: 13px; margin: 0; line-height: 1.6;">
                                <strong>🔒 Security Notice:</strong> If you didn't request this code, please ignore this email. Your account remains secure.
                            </p>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td style="background: linear-gradient(135deg, #f9fafb 0%%, #f3f4f6 100%%); padding: 30px 35px; border-top: 1px solid #e5e7eb;">
                        <p style="color: #9ca3af; font-size: 13px; text-align: center; margin: 0 0 10px 0; line-height: 1.6;">
                            Best regards,<br>
                            <strong style="color: #16a34a;">The Agro Smart Benue Team</strong>
                        </p>
                        <p style="color: #d1d5db; font-size: 11px; text-align: center; margin: 15px 0 0 0;">
                            © 2024 Agro Smart Benue. All rights reserved.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(firstName, otp);
    }

    private String buildPasswordResetOTPEmailTemplate(String firstName, String otp) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #fef2f2 0%%, #fee2e2 100%%); padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 40px rgba(0,0,0,0.1);">
                <tr>
                    <td style="background: linear-gradient(135deg, #dc2626 0%%, #b91c1c 100%%); padding: 40px 30px; text-align: center; position: relative;">
                        <h1 style="color: white; margin: 0; font-size: 32px; font-weight: 700; text-shadow: 0 2px 4px rgba(0,0,0,0.2);">Agro Smart Benue</h1>
                        <p style="color: rgba(255,255,255,0.9); margin: 12px 0 0 0; font-size: 15px;">Empowering Farmers, Growing Communities</p>
                    </td>
                </tr>
                <tr>
                    <td style="padding: 40px 35px;">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <div style="display: inline-block; background: linear-gradient(135deg, #fee2e2 0%%, #fecaca 100%%); padding: 15px; border-radius: 50%%; margin-bottom: 20px;">
                                <span style="font-size: 40px;">🔑</span>
                            </div>
                            <h2 style="color: #dc2626; margin: 0 0 10px 0; font-size: 26px; font-weight: 700;">Password Reset</h2>
                            <p style="color: #6b7280; font-size: 15px; margin: 0;">Secure your account with a new password</p>
                        </div>
                        
                        <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">Hi <strong style="color: #dc2626;">%s</strong>,</p>
                        <p style="color: #4b5563; font-size: 15px; line-height: 1.7; margin: 0 0 30px 0;">We received a request to reset your password. Use the verification code below to proceed with resetting your password. This code is unique and should be kept confidential.</p>
                        
                        <div style="background: linear-gradient(135deg, #fef2f2 0%%, #fee2e2 100%%); padding: 30px; text-align: center; margin: 30px 0; border-radius: 12px; border: 3px dashed #dc2626; box-shadow: 0 4px 12px rgba(220, 38, 38, 0.1);">
                            <p style="color: #b91c1c; font-size: 14px; font-weight: 600; margin: 0 0 15px 0; text-transform: uppercase; letter-spacing: 1px;">Password Reset Code</p>
                            <h1 style="color: #dc2626; font-size: 48px; letter-spacing: 8px; margin: 0; font-weight: 800; text-shadow: 0 2px 4px rgba(220, 38, 38, 0.1);">%s</h1>
                        </div>
                        
                        <div style="background: linear-gradient(135deg, #fef3c7 0%%, #fde68a 100%%); border-left: 4px solid #f59e0b; padding: 15px 20px; border-radius: 8px; margin: 30px 0;">
                            <p style="color: #92400e; font-size: 14px; margin: 0; line-height: 1.6;">
                                <strong>⏰ Expires in 10 minutes</strong><br>
                                Please use this code promptly to reset your password.
                            </p>
                        </div>
                        
                        <div style="background: linear-gradient(135deg, #fee2e2 0%%, #fecaca 100%%); border-left: 4px solid #dc2626; padding: 15px 20px; border-radius: 8px; margin: 20px 0 0 0;">
                            <p style="color: #991b1b; font-size: 13px; margin: 0; line-height: 1.6;">
                                <strong>🔒 Security Alert:</strong> If you didn't request a password reset, please ignore this email. Your password will remain unchanged and your account is secure.
                            </p>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td style="background: linear-gradient(135deg, #f9fafb 0%%, #f3f4f6 100%%); padding: 30px 35px; border-top: 1px solid #e5e7eb;">
                        <p style="color: #9ca3af; font-size: 13px; text-align: center; margin: 0 0 10px 0; line-height: 1.6;">
                            Best regards,<br>
                            <strong style="color: #dc2626;">The Agro Smart Benue Team</strong>
                        </p>
                        <p style="color: #d1d5db; font-size: 11px; text-align: center; margin: 15px 0 0 0;">
                            © 2024 Agro Smart Benue. All rights reserved.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(firstName, otp);
    }

    private String buildWelcomeEmailBody(String name) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #f5f7fa 0%%, #c3cfe2 100%%); padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 40px rgba(0,0,0,0.1);">
                <tr>
                    <td style="background: linear-gradient(135deg, #16a34a 0%%, #15803d 100%%); padding: 40px 30px; text-align: center; position: relative;">
                        <h1 style="color: white; margin: 0; font-size: 32px; font-weight: 700; text-shadow: 0 2px 4px rgba(0,0,0,0.2);">Agro Smart Benue</h1>
                        <p style="color: rgba(255,255,255,0.9); margin: 12px 0 0 0; font-size: 15px;">Empowering Farmers, Growing Communities</p>
                    </td>
                </tr>
                <tr>
                    <td style="padding: 40px 35px;">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <div style="display: inline-block; background: linear-gradient(135deg, #dcfce7 0%%, #bbf7d0 100%%); padding: 15px; border-radius: 50%%; margin-bottom: 20px;">
                                <span style="font-size: 40px;">🎉</span>
                            </div>
                            <h2 style="color: #16a34a; margin: 0 0 10px 0; font-size: 26px; font-weight: 700;">Welcome Aboard!</h2>
                            <p style="color: #6b7280; font-size: 15px; margin: 0;">Your journey with us begins now</p>
                        </div>
                        
                        <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">Hello <strong style="color: #16a34a;">%s</strong>,</p>
                        <p style="color: #4b5563; font-size: 15px; line-height: 1.7; margin: 0 0 30px 0;">Welcome to Agro Smart Benue! We're thrilled to have you join our agricultural community. Your account is now verified and ready to use.</p>
                        
                        <div style="background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%); padding: 25px; border-radius: 12px; margin: 30px 0; border: 2px solid #16a34a;">
                            <h3 style="color: #16a34a; margin: 0 0 20px 0; font-size: 18px; font-weight: 700;">🚀 What you can do now:</h3>
                            <table cellpadding="0" cellspacing="0" border="0" width="100%%">
                                <tr>
                                    <td style="padding: 8px 0;">
                                        <span style="color: #16a34a; font-size: 18px; margin-right: 10px;">🌾</span>
                                        <span style="color: #374151; font-size: 14px;">Browse agricultural products</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0;">
                                        <span style="color: #16a34a; font-size: 18px; margin-right: 10px;">🛒</span>
                                        <span style="color: #374151; font-size: 14px;">Place orders directly from farmers</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0;">
                                        <span style="color: #16a34a; font-size: 18px; margin-right: 10px;">🤝</span>
                                        <span style="color: #374151; font-size: 14px;">Connect with sellers and buyers</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0;">
                                        <span style="color: #16a34a; font-size: 18px; margin-right: 10px;">📚</span>
                                        <span style="color: #374151; font-size: 14px;">Get information about best farming practices</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0;">
                                        <span style="color: #16a34a; font-size: 18px; margin-right: 10px;">🌤️</span>
                                        <span style="color: #374151; font-size: 14px;">Access weather forecasts for your area</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0;">
                                        <span style="color: #16a34a; font-size: 18px; margin-right: 10px;">📊</span>
                                        <span style="color: #374151; font-size: 14px;">View market prices and trends</span>
                                    </td>
                                </tr>
                            </table>
                        </div>
                        
                        <div style="text-align: center; margin: 35px 0;">
                            <a href="https://bfpc.vercel.app/dashboard" style="background: linear-gradient(135deg, #16a34a 0%%, #15803d 100%%); color: white; padding: 16px 40px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px; box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);">
                                🏠 Go to Dashboard
                            </a>
                        </div>
                    </td>
                </tr>
                <tr>
                    <td style="background: linear-gradient(135deg, #f9fafb 0%%, #f3f4f6 100%%); padding: 30px 35px; border-top: 1px solid #e5e7eb;">
                        <p style="color: #9ca3af; font-size: 13px; text-align: center; margin: 0 0 10px 0; line-height: 1.6;">
                            Best regards,<br>
                            <strong style="color: #16a34a;">The Agro Smart Benue Support Team</strong>
                        </p>
                        <p style="color: #d1d5db; font-size: 11px; text-align: center; margin: 15px 0 0 0;">
                            © 2024 Agro Smart Benue. All rights reserved.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(name);
    }
}
