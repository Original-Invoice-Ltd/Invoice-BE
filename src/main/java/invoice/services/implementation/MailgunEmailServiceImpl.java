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

    @Override
    public void sendInvoiceNotificationEmail(String toEmail, String firstName, String invoiceId, String frontendUrl, String invoiceNumber, String invoiceDate, String dueDate, String amount, String clientName) {
        String subject = "Invoice #" + invoiceNumber + " sent to " + clientName;
        String viewInvoiceUrl = "https://originalinvoice.com/customer/invoice/" + invoiceId;
        String htmlContent = buildInvoiceNotificationEmailBody(firstName, viewInvoiceUrl, invoiceNumber, invoiceDate, dueDate, amount, clientName);

        try {
            sendEmailInternal(firstName, toEmail, subject, htmlContent);
            log.info("Invoice notification email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send invoice notification email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send invoice notification email", e);
        }
    }

    @Override
    public void sendInvoiceNotificationEmail(String toEmail, String firstName, String invoiceId, String frontendUrl) {
        String subject = "Your New Invoice is Ready";
        String viewInvoiceUrl = "https://originalinvoice.com/customer/invoice/" + invoiceId;
        String htmlContent = buildInvoiceNotificationEmailBody(firstName, viewInvoiceUrl);

        try {
            sendEmailInternal(firstName, toEmail, subject, htmlContent);
            log.info("Invoice notification email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send invoice notification email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send invoice notification email", e);
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

    private String buildVerificationEmailTemplate(String firstName, String verificationCode) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Verify your email - Original Invoice</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f8fafc; padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                <!-- Header with Logo -->
                <tr>
                    <td style="background-color: #ffffff; padding: 40px 40px 20px 40px; text-align: center; border-bottom: 1px solid #e2e8f0;">
                        <div style="display: inline-flex; align-items: center; justify-content: center; margin-bottom: 20px;">
                            <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" style="margin-right: 12px;">
                                <rect width="32" height="32" rx="9" fill="url(#paint0_linear_14310_13934)"/>
                                <rect x="0.25" y="0.25" width="31.5" height="31.5" rx="8.75" stroke="white" stroke-opacity="0.27" stroke-width="0.5"/>
                                <path d="M10.7339 8.09381C10.9572 7.70706 11.3699 7.46881 11.8165 7.46881L20.4298 7.46881C20.8764 7.46881 21.2891 7.70706 21.5124 8.09381L25.819 15.5532C26.0423 15.94 26.0423 16.4165 25.819 16.8032L22.5039 22.5452L21.4214 20.6702L24.0148 16.1782L20.069 9.34381L12.1773 9.34381L9.59725 13.8126H7.43219L10.7339 8.09381Z" fill="#EFF8FF"/>
                                <path d="M20.0875 22.9804L21.0825 24.7037C20.8892 24.822 20.6642 24.8876 20.4298 24.8876H11.8165C11.3699 24.8876 10.9572 24.6494 10.7339 24.2626L6.42723 16.8032C6.31181 16.6033 6.25606 16.3794 6.25996 16.1563L15.6132 16.1562C15.9488 16.1562 16.2589 16.3357 16.4261 16.6268L20.0818 22.9904L20.0875 22.9804Z" fill="#EFF8FF"/>
                                <defs>
                                    <linearGradient id="paint0_linear_14310_13934" x1="16" y1="0" x2="16" y2="32" gradientUnits="userSpaceOnUse">
                                        <stop stop-color="#3B82F6"/>
                                        <stop offset="1" stop-color="#1D4ED8"/>
                                    </linearGradient>
                                </defs>
                            </svg>
                            <h1 style="color: #1e293b; margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;">Original Invoice</h1>
                        </div>
                    </td>
                </tr>
                
                <!-- Main Content -->
                <tr>
                    <td style="padding: 40px;">
                        <h2 style="color: #1e293b; margin: 0 0 24px 0; font-size: 28px; font-weight: 700; line-height: 1.2;">Verify your email</h2>
                        
                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin: 0 0 8px 0;">Hi <strong>%s</strong>,</p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                            Thanks for signing up for Original Invoice.<br>
                            To verify your account, please enter the following verification code on Original Invoice:
                        </p>
                        
                        <!-- Verification Code Box -->
                        <div style="background-color: #f8fafc; border: 2px solid #e2e8f0; border-radius: 12px; padding: 32px; text-align: center; margin: 32px 0;">
                            <h1 style="color: #1e293b; margin: 0; font-size: 48px; font-weight: 800; letter-spacing: 4px; font-family: 'Courier New', monospace;">%s</h1>
                        </div>
                        
                        <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 24px 0 0 0;">
                            If you didn't create an account, you can ignore this message.
                        </p>
                        
                        <div style="margin-top: 40px; padding-top: 24px; border-top: 1px solid #e2e8f0;">
                            <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 0 0 8px 0;">
                                Best regards,<br>
                                <strong style="color: #1e293b;">The Original Invoice Team</strong>
                            </p>
                        </div>
                    </td>
                </tr>
                
                <!-- Footer -->
                <tr>
                    <td style="background-color: #f8fafc; padding: 32px 40px; text-align: center; border-top: 1px solid #e2e8f0;">
                        <div style="margin-bottom: 16px;">
                            <h3 style="color: #1e293b; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">Original Invoice</h3>
                            <p style="color: #3b82f6; margin: 0; font-size: 14px;">
                                <a href="mailto:support@originalinvoice.com" style="color: #3b82f6; text-decoration: none;">support@originalinvoice.com</a>
                            </p>
                        </div>
                        
                        <!-- Social Media Icons -->
                        <div style="margin: 20px 0;">
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #3b82f6; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">📧</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #1da1f2; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">🐦</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #0077b5; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">💼</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #1877f2; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">📘</span>
                                </div>
                            </a>
                        </div>
                        
                        <p style="color: #94a3b8; font-size: 12px; margin: 16px 0 0 0;">
                            © 2025 Original Invoice. All rights reserved.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(firstName, verificationCode);
    }

    private String buildOTPEmailTemplate(String firstName, String otp) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Verify your email - Original Invoice</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f1f5f9; padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                <!-- Header with Logo -->
                <tr>
                    <td style="background-color: #f1f5f9; padding: 40px 40px 20px 40px; text-align: center;">
                        <div style="display: inline-flex; align-items: center; justify-content: center; margin-bottom: 20px;">
                            <svg width="48" height="48" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" style="margin-right: 12px;">
                                <rect width="32" height="32" rx="9" fill="url(#paint0_linear_14310_13934)"/>
                                <rect x="0.25" y="0.25" width="31.5" height="31.5" rx="8.75" stroke="white" stroke-opacity="0.27" stroke-width="0.5"/>
                                <path d="M10.7339 8.09381C10.9572 7.70706 11.3699 7.46881 11.8165 7.46881L20.4298 7.46881C20.8764 7.46881 21.2891 7.70706 21.5124 8.09381L25.819 15.5532C26.0423 15.94 26.0423 16.4165 25.819 16.8032L22.5039 22.5452L21.4214 20.6702L24.0148 16.1782L20.069 9.34381L12.1773 9.34381L9.59725 13.8126H7.43219L10.7339 8.09381Z" fill="#EFF8FF"/>
                                <path d="M20.0875 22.9804L21.0825 24.7037C20.8892 24.822 20.6642 24.8876 20.4298 24.8876H11.8165C11.3699 24.8876 10.9572 24.6494 10.7339 24.2626L6.42723 16.8032C6.31181 16.6033 6.25606 16.3794 6.25996 16.1563L15.6132 16.1562C15.9488 16.1562 16.2589 16.3357 16.4261 16.6268L20.0818 22.9904L20.0875 22.9804Z" fill="#EFF8FF"/>
                                <defs>
                                    <linearGradient id="paint0_linear_14310_13934" x1="16" y1="0" x2="16" y2="32" gradientUnits="userSpaceOnUse">
                                        <stop stop-color="#3B82F6"/>
                                        <stop offset="1" stop-color="#1D4ED8"/>
                                    </linearGradient>
                                </defs>
                            </svg>
                        </div>
                    </td>
                </tr>
                
                <!-- Main Content -->
                <tr>
                    <td style="padding: 40px; background-color: white;">
                        <h2 style="color: #1e293b; margin: 0 0 24px 0; font-size: 32px; font-weight: 700; line-height: 1.2;">Verify your email</h2>
                        
                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin: 0 0 8px 0;">Hi <strong>%s</strong>,</p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                            Thanks for signing up for Original Invoice.
                        </p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;">
                            To verify your account, please enter the following verification code on Original Invoice:
                        </p>
                        
                        <!-- Verification Code Box -->
                        <div style="background-color: #ffffff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 40px; text-align: center; margin: 32px 0;">
                            <h1 style="color: #1e293b; margin: 0; font-size: 48px; font-weight: 800; letter-spacing: 4px; font-family: 'Courier New', monospace;">%s</h1>
                        </div>
                        
                        <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 24px 0 0 0;">
                            If you didn't create an account, you can ignore this message.
                        </p>
                        
                        <div style="margin-top: 40px; padding-top: 24px;">
                            <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 0 0 8px 0;">
                                Best regards,<br>
                                <strong style="color: #1e293b;">The Original Invoice Team</strong>
                            </p>
                        </div>
                    </td>
                </tr>
                
                <!-- Footer -->
                <tr>
                    <td style="background-color: #ffffff; padding: 32px 40px; text-align: center;">
                        <div style="margin-bottom: 16px;">
                            <h3 style="color: #1e293b; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">Original Invoice</h3>
                            <p style="color: #3b82f6; margin: 0; font-size: 14px;">
                                <a href="mailto:support@originalinvoice.com" style="color: #3b82f6; text-decoration: none;">support@originalinvoice.com</a>
                            </p>
                        </div>
                        
                        <!-- Social Media Icons -->
                        <div style="margin: 20px 0;">
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #0077b5; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">💼</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #1da1f2; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">🐦</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #1da1f2; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">❌</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #1877f2; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">📘</span>
                                </div>
                            </a>
                        </div>
                        
                        <p style="color: #94a3b8; font-size: 12px; margin: 16px 0 0 0;">
                            © 2025 Original Invoice. All rights reserved.
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
            <title>Welcome to Original Invoice</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f1f5f9; padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                <!-- Header with Logo -->
                <tr>
                    <td style="background-color: #f1f5f9; padding: 40px 40px 20px 40px; text-align: center;">
                        <div style="display: inline-flex; align-items: center; justify-content: center; margin-bottom: 20px;">
                            <svg width="48" height="48" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg" style="margin-right: 12px;">
                                <rect width="32" height="32" rx="9" fill="url(#paint0_linear_14310_13934)"/>
                                <rect x="0.25" y="0.25" width="31.5" height="31.5" rx="8.75" stroke="white" stroke-opacity="0.27" stroke-width="0.5"/>
                                <path d="M10.7339 8.09381C10.9572 7.70706 11.3699 7.46881 11.8165 7.46881L20.4298 7.46881C20.8764 7.46881 21.2891 7.70706 21.5124 8.09381L25.819 15.5532C26.0423 15.94 26.0423 16.4165 25.819 16.8032L22.5039 22.5452L21.4214 20.6702L24.0148 16.1782L20.069 9.34381L12.1773 9.34381L9.59725 13.8126H7.43219L10.7339 8.09381Z" fill="#EFF8FF"/>
                                <path d="M20.0875 22.9804L21.0825 24.7037C20.8892 24.822 20.6642 24.8876 20.4298 24.8876H11.8165C11.3699 24.8876 10.9572 24.6494 10.7339 24.2626L6.42723 16.8032C6.31181 16.6033 6.25606 16.3794 6.25996 16.1563L15.6132 16.1562C15.9488 16.1562 16.2589 16.3357 16.4261 16.6268L20.0818 22.9904L20.0875 22.9804Z" fill="#EFF8FF"/>
                                <defs>
                                    <linearGradient id="paint0_linear_14310_13934" x1="16" y1="0" x2="16" y2="32" gradientUnits="userSpaceOnUse">
                                        <stop stop-color="#3B82F6"/>
                                        <stop offset="1" stop-color="#1D4ED8"/>
                                    </linearGradient>
                                </defs>
                            </svg>
                        </div>
                    </td>
                </tr>
                
                <!-- Main Content -->
                <tr>
                    <td style="padding: 40px; background-color: white;">
                        <h2 style="color: #1e293b; margin: 0 0 24px 0; font-size: 32px; font-weight: 700; line-height: 1.2;">Welcome to Original Invoice</h2>
                        
                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin: 0 0 8px 0;">Hi <strong>%s</strong>,</p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                            Welcome to Original Invoice, we're glad to have you here.
                        </p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                            Your account is now active, and you can start creating invoices, managing clients, and tracking payments right away.
                        </p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;">
                            Everything is designed to help you work faster and stay organised with less effort.
                        </p>
                        
                        <!-- CTA Button -->
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="#" style="background: linear-gradient(135deg, #3b82f6 0%%, #1d4ed8 100%%); color: white; padding: 16px 32px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);">
                                Go to Dashboard
                            </a>
                        </div>
                        
                        <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 32px 0 0 0;">
                            If you ever need help, we're always here to support you.
                        </p>
                        
                        <div style="margin-top: 40px; padding-top: 24px;">
                            <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 0 0 8px 0;">
                                Best regards,<br>
                                <strong style="color: #1e293b;">The Original Invoice Team</strong>
                            </p>
                        </div>
                    </td>
                </tr>
                
                <!-- Footer -->
                <tr>
                    <td style="background-color: #ffffff; padding: 32px 40px; text-align: center;">
                        <div style="margin-bottom: 16px;">
                            <h3 style="color: #1e293b; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">Original Invoice</h3>
                            <p style="color: #3b82f6; margin: 0; font-size: 14px;">
                                <a href="mailto:support@originalinvoice.com" style="color: #3b82f6; text-decoration: none;">support@originalinvoice.com</a>
                            </p>
                        </div>
                        
                        <!-- Social Media Icons -->
                        <div style="margin: 20px 0;">
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #0077b5; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">💼</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #E1306C; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">📷</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #1da1f2; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">❌</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #1877f2; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">📘</span>
                                </div>
                            </a>
                        </div>
                        
                        <p style="color: #94a3b8; font-size: 12px; margin: 16px 0 0 0;">
                            © 2025 Original Invoice. All rights reserved.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(name);
    }

    private String buildInvoiceNotificationEmailBody(String firstName, String viewInvoiceUrl) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>New Invoice - Original Invoice</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #EFF8FF80; padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto;">
                <!-- Header with Logo -->
                <tr>
                    <td style="background-color: #EFF8FF80; padding: 40px 40px 20px 40px; text-align: center;">
                        <div style="display: inline-flex; align-items: center; justify-content: center; margin-bottom: 20px;">
                            <svg width="120" height="40" viewBox="0 0 120 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <rect width="32" height="32" rx="9" fill="url(#paint0_linear_14310_13934)"/>
                                <rect x="0.25" y="0.25" width="31.5" height="31.5" rx="8.75" stroke="white" stroke-opacity="0.27" stroke-width="0.5"/>
                                <path d="M10.7339 8.09381C10.9572 7.70706 11.3699 7.46881 11.8165 7.46881L20.4298 7.46881C20.8764 7.46881 21.2891 7.70706 21.5124 8.09381L25.819 15.5532C26.0423 15.94 26.0423 16.4165 25.819 16.8032L22.5039 22.5452L21.4214 20.6702L24.0148 16.1782L20.069 9.34381L12.1773 9.34381L9.59725 13.8126H7.43219L10.7339 8.09381Z" fill="#EFF8FF"/>
                                <path d="M20.0875 22.9804L21.0825 24.7037C20.8892 24.822 20.6642 24.8876 20.4298 24.8876H11.8165C11.3699 24.8876 10.9572 24.6494 10.7339 24.2626L6.42723 16.8032C6.31181 16.6033 6.25606 16.3794 6.25996 16.1563L15.6132 16.1562C15.9488 16.1562 16.2589 16.3357 16.4261 16.6268L20.0818 22.9904L20.0875 22.9804Z" fill="#EFF8FF"/>
                                <defs>
                                    <linearGradient id="paint0_linear_14310_13934" x1="16" y1="0" x2="16" y2="32" gradientUnits="userSpaceOnUse">
                                        <stop stop-color="#3B82F6"/>
                                        <stop offset="1" stop-color="#1D4ED8"/>
                                    </linearGradient>
                                </defs>
                            </svg>
                        </div>
                    </td>
                </tr>
                
                <!-- Main Content - White Background -->
                <tr>
                    <td style="padding: 40px; background-color: white; border-radius: 12px; margin: 20px;">
                        <h2 style="color: #1e293b; margin: 0 0 24px 0; font-size: 28px; font-weight: 700; line-height: 1.2;">You have received a new invoice</h2>
                        
                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin: 0 0 8px 0;">Hi <strong>%s</strong>,</p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;">
                            A new invoice has been sent to you. Please review the details and proceed with payment if required.
                        </p>
                        
                        <!-- CTA Button -->
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="%s" style="background-color: #2F80ED; color: white; padding: 16px 32px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px;">
                                View Invoice
                            </a>
                        </div>
                        
                        <div style="text-align: center; margin: 24px 0;">
                            <p style="color: #64748b; font-size: 12px; margin: 0;">
                                Powered by <span style="color: #2F80ED; font-weight: 600;">Original Invoice</span>
                            </p>
                        </div>
                        
                        <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 32px 0 0 0;">
                            If you have any questions about this invoice, please contact the sender directly.
                        </p>
                        
                        <div style="margin-top: 40px; padding-top: 24px;">
                            <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 0 0 8px 0;">
                                Best regards,<br>
                                <strong style="color: #1e293b;">The Original Invoice Team</strong>
                            </p>
                        </div>
                    </td>
                </tr>
                
                <!-- Footer -->
                <tr>
                    <td style="background-color: #EFF8FF80; padding: 32px 40px; text-align: center;">
                        <div style="margin-bottom: 16px;">
                            <h3 style="color: #1e293b; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">Original Invoice</h3>
                            <p style="color: #3b82f6; margin: 0; font-size: 14px;">
                                <a href="mailto:support@originalinvoice.com" style="color: #3b82f6; text-decoration: none;">support@originalinvoice.com</a>
                            </p>
                        </div>
                        
                        <!-- Social Media Icons -->
                        <div style="margin: 20px 0;">
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #0077b5; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">💼</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #E1306C; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">📷</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #1da1f2; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">❌</span>
                                </div>
                            </a>
                            <a href="#" style="display: inline-block; margin: 0 8px; text-decoration: none;">
                                <div style="width: 32px; height: 32px; background-color: #1877f2; border-radius: 6px; display: inline-flex; align-items: center; justify-content: center;">
                                    <span style="color: white; font-size: 16px;">📘</span>
                                </div>
                            </a>
                        </div>
                        
                        <p style="color: #94a3b8; font-size: 12px; margin: 16px 0 0 0;">
                            © 2025 Original Invoice. All rights reserved.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(firstName, viewInvoiceUrl);
    }

    private String buildInvoiceNotificationEmailBody(String firstName, String viewInvoiceUrl, String invoiceNumber, String invoiceDate, String dueDate, String amount, String clientName) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Invoice #%s sent to %s</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background: linear-gradient(135deg, #e0f2fe 0%%, #bfdbfe 100%%); padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                <!-- Header with Logo -->
                <tr>
                    <td style="background-color: #EFF8FF80; padding: 40px 40px 20px 40px; text-align: center;">
                        <div style="display: inline-flex; align-items: center; justify-content: center; margin-bottom: 20px;">
                            <svg width="120" height="40" viewBox="0 0 120 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <text x="60" y="25" text-anchor="middle" style="font-family: Arial, sans-serif; font-size: 18px; font-weight: bold; fill: #2F80ED;">Original Invoice</text>
                            </svg>
                        </div>
                    </td>
                </tr>
                
                <!-- Main Content - White Background -->
                <tr>
                    <td style="padding: 40px; background-color: white; border-radius: 12px; margin: 20px;">
                        <h2 style="color: #1e293b; margin: 0 0 24px 0; font-size: 28px; font-weight: 700; line-height: 1.2;">Invoice #%s sent to %s</h2>
                        
                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin: 0 0 8px 0;">Hi %s,</p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;">
                            Your invoice #%s for %s has been successfully sent to %s.
                        </p>
                        
                        <!-- Invoice Amount Box -->
                        <div style="text-align: center; margin: 32px 0;">
                            <p style="color: #64748b; font-size: 14px; margin: 0 0 12px 0; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600;">INVOICE AMOUNT</p>
                            <h1 style="color: #1e293b; margin: 0 0 24px 0; font-size: 36px; font-weight: 800;">%s</h1>
                        </div>
                        
                        <!-- Invoice Details Table -->
                        <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="font-size: 14px; margin: 24px 0;">
                            <tr>
                                <td style="color: #64748b; padding: 12px 0; width: 40%%; font-weight: 500;">Invoice No</td>
                                <td style="color: #1e293b; padding: 12px 0; text-align: right; font-weight: 600;">%s</td>
                            </tr>
                            <tr>
                                <td style="color: #64748b; padding: 12px 0; font-weight: 500;">Invoice Date</td>
                                <td style="color: #1e293b; padding: 12px 0; text-align: right; font-weight: 600;">%s</td>
                            </tr>
                            <tr>
                                <td style="color: #64748b; padding: 12px 0; font-weight: 500;">Due Date</td>
                                <td style="color: #1e293b; padding: 12px 0; text-align: right; font-weight: 600;">%s</td>
                            </tr>
                        </table>
                        
                        <!-- CTA Button -->
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="%s" style="background: #3b82f6; color: white; padding: 14px 40px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);">
                                View Invoice
                            </a>
                        </div>
                        
                        <div style="text-align: center; margin: 24px 0;">
                            <p style="color: #64748b; font-size: 12px; margin: 0;">
                                Powered by <span style="color: #2F80ED; font-weight: 600;">Original Invoice</span>
                            </p>
                        </div>
                        
                        <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 32px 0 0 0; text-align: center;">
                            Powered by <a href="#" style="color: #3b82f6; text-decoration: none;">Original Invoice</a>
                        </p>
                        
                        <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 32px 0 0 0;">
                            We'll notify you once your client views or pays this invoice.
                        </p>
                        
                        <div style="margin-top: 40px; padding-top: 24px;">
                            <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 0 0 8px 0;">
                                Warm regards,<br>
                                <strong style="color: #1e293b;">The Original Invoice Team</strong>
                            </p>
                        </div>
                    </td>
                </tr>
                
                <!-- Footer -->
                <tr>
                    <td style="background: linear-gradient(135deg, #e0f2fe 0%%, #bfdbfe 100%%); padding: 32px 40px; text-align: center;">
                        <div style="margin-bottom: 16px;">
                            <h3 style="color: #1e293b; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">Original Invoice</h3>
                            <p style="color: #3b82f6; margin: 0; font-size: 14px;">
                                <a href="mailto:support@originalinvoice.com" style="color: #3b82f6; text-decoration: none;">support@originalinvoice.com</a>
                            </p>
                        </div>
                        
                        <!-- Social Media Icons with SVGs -->
                        <div style="margin: 20px 0;">
                            <!-- LinkedIn -->
                            <a href="#" style="display: inline-block; margin: 0 6px; text-decoration: none;">
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <rect x="0.5" y="0.5" width="23" height="23" rx="3.5" fill="#BFE2FE"/>
                                    <rect x="0.5" y="0.5" width="23" height="23" rx="3.5" stroke="white"/>
                                    <g clip-path="url(#clip0)">
                                        <path d="M18.8189 4H5.18111C4.86786 4 4.56744 4.12444 4.34594 4.34594C4.12444 4.56744 4 4.86786 4 5.18111V18.8189C4 19.1321 4.12444 19.4326 4.34594 19.6541C4.56744 19.8756 4.86786 20 5.18111 20H18.8189C19.1321 20 19.4326 19.8756 19.6541 19.6541C19.8756 19.4326 20 19.1321 20 18.8189V5.18111C20 4.86786 19.8756 4.56744 19.6541 4.34594C19.4326 4.12444 19.1321 4 18.8189 4ZM8.76889 17.63H6.36333V9.98889H8.76889V17.63ZM7.56444 8.93C7.29158 8.92846 7.02528 8.84613 6.79916 8.69339C6.57304 8.54065 6.39723 8.32435 6.29392 8.07179C6.19061 7.81923 6.16443 7.54173 6.21869 7.2743C6.27294 7.00688 6.4052 6.76152 6.59877 6.56919C6.79234 6.37686 7.03854 6.24618 7.30631 6.19364C7.57408 6.1411 7.85141 6.16906 8.1033 6.27399C8.35519 6.37892 8.57036 6.55611 8.72164 6.78321C8.87293 7.01031 8.95355 7.27713 8.95333 7.55C8.95591 7.73269 8.92167 7.91403 8.85267 8.0832C8.78368 8.25238 8.68132 8.40593 8.55171 8.53471C8.42211 8.66349 8.2679 8.76486 8.09828 8.83277C7.92867 8.90068 7.74711 8.93375 7.56444 8.93ZM17.6356 17.6367H15.2311V13.4622C15.2311 12.2311 14.7078 11.8511 14.0322 11.8511C13.3189 11.8511 12.6189 12.3889 12.6189 13.4933V17.6367H10.2133V9.99445H12.5267V11.0533H12.5578C12.79 10.5833 13.6033 9.78 14.8444 9.78C16.1867 9.78 17.6367 10.5767 17.6367 12.91L17.6356 17.6367Z" fill="#0A66C2"/>
                                    </g>
                                    <defs>
                                        <clipPath id="clip0">
                                            <rect width="16" height="16" fill="white" transform="translate(4 4)"/>
                                        </clipPath>
                                    </defs>
                                </svg>
                            </a>
                            
                            <!-- Instagram -->
                            <a href="#" style="display: inline-block; margin: 0 6px; text-decoration: none;">
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <rect x="0.5" y="0.5" width="23" height="23" rx="3.5" fill="#BFE2FE"/>
                                    <rect x="0.5" y="0.5" width="23" height="23" rx="3.5" stroke="white"/>
                                    <g clip-path="url(#clip1)">
                                        <path d="M12 5.44062C14.1375 5.44062 14.3906 5.45 15.2313 5.4875C16.0125 5.52187 16.4344 5.65313 16.7156 5.7625C17.0875 5.90625 17.3563 6.08125 17.6344 6.35938C17.9156 6.64062 18.0875 6.90625 18.2313 7.27813C18.3406 7.55938 18.4719 7.98438 18.5063 8.7625C18.5438 9.60625 18.5531 9.85938 18.5531 11.9938C18.5531 14.1313 18.5438 14.3844 18.5063 15.225C18.4719 16.0063 18.3406 16.4281 18.2313 16.7094C18.0875 17.0813 17.9125 17.35 17.6344 17.6281C17.3531 17.9094 17.0875 18.0813 16.7156 18.225C16.4344 18.3344 16.0094 18.4656 15.2313 18.5C14.3875 18.5375 14.1344 18.5469 12 18.5469C9.8625 18.5469 9.60938 18.5375 8.76875 18.5C7.9875 18.4656 7.56563 18.3344 7.28438 18.225C6.9125 18.0813 6.64375 17.9063 6.36563 17.6281C6.08438 17.3469 5.9125 17.0813 5.76875 16.7094C5.65938 16.4281 5.52813 16.0031 5.49375 15.225C5.45625 14.3813 5.44688 14.1281 5.44688 11.9938C5.44688 9.85625 5.45625 9.60313 5.49375 8.7625C5.52813 7.98125 5.65938 7.55938 5.76875 7.27813C5.9125 6.90625 6.0875 6.6375 6.36563 6.35938C6.64688 6.07812 6.9125 5.90625 7.28438 5.7625C7.56563 5.65313 7.99063 5.52187 8.76875 5.4875C9.60938 5.45 9.8625 5.44062 12 5.44062ZM12 4C9.82813 4 9.55625 4.00937 8.70313 4.04688C7.85313 4.08438 7.26875 4.22187 6.7625 4.41875C6.23438 4.625 5.7875 4.89688 5.34375 5.34375C4.89688 5.7875 4.625 6.23438 4.41875 6.75938C4.22187 7.26875 4.08438 7.85 4.04688 8.7C4.00938 9.55625 4 9.82812 4 12C4 14.1719 4.00938 14.4438 4.04688 15.2969C4.08438 16.1469 4.22187 16.7313 4.41875 17.2375C4.625 17.7656 4.89688 18.2125 5.34375 18.6562C5.7875 19.1 6.23438 19.375 6.75938 19.5781C7.26875 19.775 7.85 19.9125 8.7 19.95C9.55313 19.9875 9.825 19.9969 11.9969 19.9969C14.1688 19.9969 14.4406 19.9875 15.2938 19.95C16.1438 19.9125 16.7281 19.775 17.2344 19.5781C17.7594 19.375 18.2063 19.1 18.65 18.6562C19.0938 18.2125 19.3688 17.7656 19.5719 17.2406C19.7688 16.7313 19.9063 16.15 19.9438 15.3C19.9813 14.4469 19.9906 14.175 19.9906 12.0031C19.9906 9.83125 19.9813 9.55938 19.9438 8.70625C19.9063 7.85625 19.7688 7.27188 19.5719 6.76562C19.375 6.23438 19.1031 5.7875 18.6563 5.34375C18.2125 4.9 17.7656 4.625 17.2406 4.42188C16.7313 4.225 16.15 4.0875 15.3 4.05C14.4438 4.00938 14.1719 4 12 4Z" fill="#000100"/>
                                        <path d="M12 7.89062C9.73125 7.89062 7.89062 9.73125 7.89062 12C7.89062 14.2688 9.73125 16.1094 12 16.1094C14.2688 16.1094 16.1094 14.2688 16.1094 12C16.1094 9.73125 14.2688 7.89062 12 7.89062ZM12 14.6656C10.5281 14.6656 9.33437 13.4719 9.33437 12C9.33437 10.5281 10.5281 9.33437 12 9.33437C13.4719 9.33437 14.6656 10.5281 14.6656 12C14.6656 13.4719 13.4719 14.6656 12 14.6656Z" fill="#000100"/>
                                        <path d="M17.2312 7.72818C17.2312 8.25943 16.8 8.68755 16.2719 8.68755C15.7406 8.68755 15.3125 8.2563 15.3125 7.72818C15.3125 7.19692 15.7438 6.7688 16.2719 6.7688C16.8 6.7688 17.2312 7.20005 17.2312 7.72818Z" fill="#000100"/>
                                    </g>
                                    <defs>
                                        <clipPath id="clip1">
                                            <rect width="16" height="16" fill="white" transform="translate(4 4)"/>
                                        </clipPath>
                                    </defs>
                                </svg>
                            </a>
                            
                            <!-- X (Twitter) -->
                            <a href="#" style="display: inline-block; margin: 0 6px; text-decoration: none;">
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <rect x="0.5" y="0.5" width="23" height="23" rx="3.5" fill="#BFE2FE"/>
                                    <rect x="0.5" y="0.5" width="23" height="23" rx="3.5" stroke="white"/>
                                    <path d="M18 5L7 18M18 18L7 5" stroke="#000" stroke-width="2" stroke-linecap="round"/>
                                </svg>
                            </a>
                            
                            <!-- Facebook -->
                            <a href="#" style="display: inline-block; margin: 0 6px; text-decoration: none;">
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <rect x="0.5" y="0.5" width="23" height="23" rx="3.5" fill="#BFE2FE"/>
                                    <rect x="0.5" y="0.5" width="23" height="23" rx="3.5" stroke="white"/>
                                    <g clip-path="url(#clip2)">
                                        <path d="M20 12C20 7.58176 16.4182 4 12 4C7.58176 4 4 7.58176 4 12C4 15.7517 6.58304 18.8998 10.0675 19.7645V14.4448H8.41792V12H10.0675V10.9466C10.0675 8.22368 11.2998 6.9616 13.9731 6.9616C14.48 6.9616 15.3546 7.06112 15.7123 7.16032V9.37632C15.5235 9.35648 15.1955 9.34656 14.7882 9.34656C13.4765 9.34656 12.9696 9.84352 12.9696 11.1354V12H15.5827L15.1338 14.4448H12.9696V19.9414C16.9309 19.463 20.0003 16.0902 20.0003 12H20Z" fill="#0866FF"/>
                                        <path d="M15.1299 14.4447L15.5789 11.9999H12.9657V11.1353C12.9657 9.84347 13.4726 9.34651 14.7843 9.34651C15.1917 9.34651 15.5197 9.35643 15.7085 9.37627V7.16027C15.3507 7.06075 14.4761 6.96155 13.9693 6.96155C11.296 6.96155 10.0637 8.22363 10.0637 10.9465V11.9999H8.41406V14.4447H10.0637V19.7644C10.6825 19.918 11.3299 19.9999 11.9961 19.9999C12.3241 19.9999 12.6477 19.9798 12.9654 19.9414V14.4447H15.1296H15.1299Z" fill="white"/>
                                    </g>
                                    <defs>
                                        <clipPath id="clip2">
                                            <rect width="16" height="16" fill="white" transform="translate(4 4)"/>
                                        </clipPath>
                                    </defs>
                                </svg>
                            </a>
                        </div>
                        
                        <p style="color: #94a3b8; font-size: 12px; margin: 16px 0 0 0;">
                            © 2025 Original Invoice. All rights reserved.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(invoiceNumber, clientName, invoiceNumber, clientName, firstName, invoiceNumber, amount, clientName, amount, invoiceNumber, invoiceDate, dueDate, viewInvoiceUrl);
    }
}
