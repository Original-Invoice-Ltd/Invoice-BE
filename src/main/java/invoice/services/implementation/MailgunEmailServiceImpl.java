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
        <title>Invoice #%s for %s</title>
    </head>
    <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background: linear-gradient(135deg, #e0f2fe 0%%, #bfdbfe 100%%); padding: 40px 20px;">
        <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
            <tr>
                <td style="background-color: #EFF8FF80; padding: 40px 40px 20px 40px; text-align: center;">
                    <div style="display: inline-flex; align-items: center; justify-content: center; margin-bottom: 20px;">
                        <svg width="120" height="40" viewBox="0 0 120 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <text x="60" y="25" text-anchor="middle" style="font-family: Arial, sans-serif; font-size: 18px; font-weight: bold; fill: #2F80ED;">Original Invoice</text>
                        </svg>
                    </div>
                </td>
            </tr>
            <tr>
                <td style="padding: 40px; background-color: white; border-radius: 12px; margin: 20px;">
                    <h2 style="color: #1e293b; margin: 0 0 24px 0; font-size: 28px; font-weight: 700; line-height: 1.2;">Your invoice is ready</h2>
                    <p style="color: #475569; font-size: 16px; line-height: 1.6; margin: 0 0 8px 0;">Hi %s,</p>
                    <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;"> %s has sent you invoice #%s for <strong>N %s</strong>. Use the button below to view the details and complete your payment.</p>
                    <div style="text-align: center; margin: 32px 0;">
                        <p style="color: #64748b; font-size: 14px; margin: 0 0 12px 0; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600;">AMOUNT DUE</p>
                        <h1 style="color: #1e293b; margin: 0 0 24px 0; font-size: 36px; font-weight: 800;">%s</h1>
                    </div>
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
                    <div style="text-align: center; margin: 32px 0;">
                        <a href="%s" style="background: #3b82f6; color: white; padding: 14px 40px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);"> View and Pay Invoice</a>
                    </div>
                    <div style="text-align: center; margin: 24px 0;">
                        <p style="color: #64748b; font-size: 12px; margin: 0;"> Powered by <span style="color: #2F80ED; font-weight: 600;">Original Invoice</span></p>
                    </div>
                    <div style="margin-top: 40px; padding-top: 24px; border-top: 1px solid #f1f5f9;">
                        <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 0 0 8px 0;">  Best regards,<br> <strong style="color: #1e293b;">The Original Invoice Team</strong></p>
                    </div>
                </td>
            </tr>
            <tr>
                <td style="background: linear-gradient(135deg, #e0f2fe 0%%, #bfdbfe 100%%); padding: 32px 40px; text-align: center;">
                    <div style="margin-bottom: 16px;">
                        <h3 style="color: #1e293b; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">Original Invoice</h3>
                        <p style="color: #3b82f6; margin: 0; font-size: 14px;"> <a href="mailto:support@originalinvoice.com" style="color: #3b82f6; text-decoration: none;">support@originalinvoice.com</a></p>
                    </div>
                    <div style="margin: 20px 0;">
                        <a href="#" style="display: inline-block; margin: 0 6px; text-decoration: none;">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none"><rect width="23" height="23" x=".5" y=".5" fill="#BFE2FE" rx="3.5"/><rect width="23" height="23" x=".5" y=".5" stroke="#fff" rx="3.5"/><g clip-path="url(#a_li)"><path fill="#0A66C2" d="M18.819 4H5.18A1.181 1.181 0 0 0 4 5.181V18.82A1.181 1.181 0 0 0 5.181 20H18.82A1.181 1.181 0 0 0 20 18.819V5.18A1.181 1.181 0 0 0 18.819 4ZM8.769 17.63H6.363V9.989H8.77v7.641Zm-1.205-8.7a1.381 1.381 0 1 1 1.39-1.38 1.361 1.361 0 0 1-1.39 1.38Zm10.072 8.707H15.23v-4.175c0-1.23-.523-1.61-1.199-1.61-.713 0-1.413.537-1.413 1.641v4.144h-2.406V9.994h2.314v1.06h.03c.233-.47 1.046-1.274 2.287-1.274 1.343 0 2.793.797 2.793 3.13l-.001 4.727Z"/></g><defs><clipPath id="a_li"><path fill="#fff" d="M4 4h16v16H4z"/></clipPath></defs></svg>
                        </a>
                        <a href="#" style="display: inline-block; margin: 0 6px; text-decoration: none;">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none"><rect width="23" height="23" x=".5" y=".5" fill="#BFE2FE" rx="3.5"/><rect width="23" height="23" x=".5" y=".5" stroke="#fff" rx="3.5"/><g fill="#000100" clip-path="url(#a_ig)"><path d="M12 5.44c2.137 0 2.39.01 3.231.047.781.035 1.203.166 1.485.276.371.143.64.318.918.596.282.282.453.547.597.92.11.28.24.705.275 1.483.038.844.047 1.097.047 3.232 0 2.137-.01 2.39-.047 3.231-.034.781-.165 1.203-.275 1.484-.144.372-.319.641-.597.92a2.46 2.46 0 0 1-.918.596c-.282.11-.707.24-1.485.275-.844.038-1.097.047-3.231.047-2.137 0-2.39-.01-3.231-.047-.782-.034-1.203-.166-1.485-.275a2.472 2.472 0 0 1-.918-.597 2.46 2.46 0 0 1-.597-.919c-.11-.28-.24-.706-.275-1.484-.038-.844-.047-1.097-.047-3.231 0-2.138.01-2.39.047-3.232.034-.78.165-1.203.275-1.484.143-.372.319-.64.597-.919a2.46 2.46 0 0 1 .918-.596c.282-.11.707-.241 1.485-.276.84-.037 1.094-.046 3.231-.046ZM12 4c-2.172 0-2.444.01-3.297.047-.85.037-1.434.175-1.94.372a3.905 3.905 0 0 0-1.42.925 3.92 3.92 0 0 0-.924 1.415c-.197.51-.335 1.091-.372 1.941C4.009 9.556 4 9.828 4 12c0 2.172.01 2.444.047 3.297.037.85.175 1.434.372 1.94.206.529.478.976.925 1.42.443.443.89.718 1.415.921.51.197 1.091.335 1.941.372.853.038 1.125.047 3.297.047s2.444-.01 3.297-.047c.85-.037 1.434-.175 1.94-.372a3.91 3.91 0 0 0 1.416-.922 3.91 3.91 0 0 0 .922-1.415c.197-.51.334-1.091.372-1.941.037-.853.047-1.125.047-3.297s-.01-2.444-.047-3.297c-.038-.85-.175-1.434-.372-1.94a3.748 3.748 0 0 0-.916-1.422 3.911 3.911 0 0 0-1.415-.922c-.51-.197-1.091-.334-1.941-.372C14.444 4.01 14.172 4 12 4Z"/><path d="M12 7.89a4.11 4.11 0 0 0 0 8.22 4.11 4.11 0 0 0 0-8.22Zm0 6.776a2.666 2.666 0 1 1 0-5.332 2.666 2.666 0 0 1 0 5.332ZM17.231 7.728a.96.96 0 1 1-1.919 0 .96.96 0 0 1 1.92 0Z"/></g><defs><clipPath id="a_ig"><path fill="#fff" d="M4 4h16v16H4z"/></clipPath></defs></svg>
                        </a>
                        <a href="#" style="display: inline-block; margin: 0 6px; text-decoration: none;">
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none"><rect width="23" height="23" x=".5" y=".5" fill="#BFE2FE" rx="3.5"/><rect width="23" height="23" x=".5" y=".5" stroke="#fff" rx="3.5"/><g clip-path="url(#a_x)"><path fill="#000" d="M16.215 5.27h2.249l-4.913 5.615 5.78 7.642h-4.526l-3.545-4.635-4.056 4.635h-2.25l5.255-6.007-5.545-7.25h4.64l3.205 4.236 3.706-4.237Zm-.79 11.91h1.247L8.628 6.545H7.29l8.136 10.635Z"/></g><defs><clipPath id="a_x"><path fill="#fff" d="M4 4h16v16H4z"/></clipPath></defs></svg>
                        </a>
                        <a href="#" style="display: inline-block; margin: 0 6px; text-decoration: none;">
                            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" fill="none"><rect width="23" height="23" x=".5" y=".5" fill="#BFE2FE" rx="3.5"/><rect width="23" height="23" x=".5" y=".5" stroke="#fff" rx="3.5"/><g clip-path="url(#a_fb)"><path fill="#0866FF" d="M20 12a8 8 0 1 0-9.932 7.765v-5.32h-1.65V12h1.65v-1.053c0-2.723 1.232-3.985 3.905-3.985.507 0 1.382.1 1.74.198v2.216c-.19-.02-.518-.03-.925-.03-1.312 0-1.818.498-1.818 1.79V12h2.613l-.45 2.445H12.97v5.496A8 8 0 0 0 20 12Z"/><path fill="#fff" d="M15.13 14.445 15.579 12h-2.613v-.865c0-1.292.507-1.788 1.818-1.788.408 0 .736.01.925.03V7.16c-.358-.1-1.233-.198-1.74-.198-2.673 0-3.905 1.262-3.905 3.985V12h-1.65v2.445h1.65v5.32a8.013 8.013 0 0 0 2.901.176v-5.496h2.165Z"/></g><defs><clipPath id="a_fb"><path fill="#fff" d="M4 4h16v16H4z"/></clipPath></defs></svg>
                        </a>
                    </div>
                    <p style="color: #94a3b8; font-size: 12px; margin: 16px 0 0 0;">© 2026 Original Invoice. All rights reserved.</p>
                </td>
            </tr>
        </table>
    </body>
    </html>
    """.formatted(invoiceNumber,clientName, clientName, firstName, invoiceNumber, amount, amount, invoiceNumber, invoiceDate.replace("T"," "), dueDate.replace("T"," "), viewInvoiceUrl);
}

    @Override
    public void sendPaymentEvidenceNotificationEmail(String toEmail, String senderName, String invoiceNumber, String customerName, String dashboardUrl) {
        String subject = "Payment Evidence Uploaded - Invoice #" + invoiceNumber;
        String htmlContent = buildPaymentEvidenceNotificationEmailBody(senderName, invoiceNumber, customerName, dashboardUrl);

        try {
            sendEmailInternal(senderName, toEmail, subject, htmlContent);
            log.info("Payment evidence notification email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send payment evidence notification email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send payment evidence notification email", e);
        }
    }

    private String buildPaymentEvidenceNotificationEmailBody(String senderName, String invoiceNumber, String customerName, String dashboardUrl) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Payment Evidence Uploaded - Invoice #%s</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%); padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08);">
                <!-- Header with Logo -->
                <tr>
                    <td style="background: linear-gradient(135deg, #22c55e 0%%, #16a34a 100%%); padding: 40px 40px 20px 40px; text-align: center;">
                        <div style="display: inline-flex; align-items: center; justify-content: center; margin-bottom: 20px;">
                            <div style="background-color: rgba(255,255,255,0.2); padding: 12px; border-radius: 50%%; margin-right: 12px;">
                                <span style="font-size: 32px;">📄</span>
                            </div>
                            <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 700; text-shadow: 0 2px 4px rgba(0,0,0,0.2);">Original Invoice</h1>
                        </div>
                    </td>
                </tr>
                
                <!-- Main Content -->
                <tr>
                    <td style="padding: 40px; background-color: white;">
                        <div style="text-align: center; margin-bottom: 30px;">
                            <div style="display: inline-block; background: linear-gradient(135deg, #dcfce7 0%%, #bbf7d0 100%%); padding: 15px; border-radius: 50%%; margin-bottom: 20px;">
                                <span style="font-size: 40px;">💰</span>
                            </div>
                            <h2 style="color: #16a34a; margin: 0 0 10px 0; font-size: 28px; font-weight: 700;">Payment Evidence Uploaded!</h2>
                            <p style="color: #6b7280; font-size: 15px; margin: 0;">Your customer has submitted proof of payment</p>
                        </div>
                        
                        <p style="color: #374151; font-size: 16px; line-height: 1.6; margin: 0 0 20px 0;">Hi <strong style="color: #16a34a;">%s</strong>,</p>
                        
                        <p style="color: #4b5563; font-size: 16px; line-height: 1.7; margin: 0 0 30px 0;">
                            Great news! <strong>%s</strong> has uploaded proof of payment for Invoice <strong>#%s</strong>. 
                            The invoice status has been updated to "Pending" and is ready for your review.
                        </p>
                        
                        <!-- Invoice Details Box -->
                        <div style="background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%); padding: 25px; border-radius: 12px; border: 2px solid #22c55e; margin: 30px 0; text-align: center;">
                            <p style="color: #15803d; font-size: 14px; font-weight: 600; margin: 0 0 10px 0; text-transform: uppercase; letter-spacing: 1px;">Invoice Number</p>
                            <h3 style="color: #16a34a; font-size: 24px; font-weight: 800; margin: 0 0 15px 0;">#%s</h3>
                            <p style="color: #15803d; font-size: 14px; margin: 0;"><strong>Customer:</strong> %s</p>
                        </div>
                        
                        <!-- CTA Button -->
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="https://api.originalinvoice.com/dashboard/payment" style="background: linear-gradient(135deg, #22c55e 0%%, #16a34a 100%%); color: white; padding: 16px 32px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px; box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);">
                                Review Payment Evidence
                            </a>
                        </div>
                        
                        <div style="background: linear-gradient(135deg, #fef3c7 0%%, #fde68a 100%%); border-left: 4px solid #f59e0b; padding: 15px 20px; border-radius: 8px; margin: 30px 0;">
                            <p style="color: #92400e; font-size: 14px; margin: 0; line-height: 1.6;">
                                <strong>⏰ Next Steps:</strong> Please review the uploaded payment evidence and update the invoice status accordingly (Paid/Rejected) from your dashboard.
                            </p>
                        </div>
                        
                        <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 32px 0 0 0;">
                            You can access your dashboard anytime to manage all your invoices and payments.
                        </p>
                        
                        <div style="margin-top: 40px; padding-top: 24px; border-top: 1px solid #e5e7eb;">
                            <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 0 0 8px 0;">
                                Best regards,<br>
                                <strong style="color: #16a34a;">The Original Invoice Team</strong>
                            </p>
                        </div>
                    </td>
                </tr>
                
                <!-- Footer -->
                <tr>
                    <td style="background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%); padding: 32px 40px; text-align: center; border-top: 1px solid #e5e7eb;">
                        <div style="margin-bottom: 16px;">
                            <h3 style="color: #16a34a; margin: 0 0 8px 0; font-size: 16px; font-weight: 600;">Original Invoice</h3>
                            <p style="color: #22c55e; margin: 0; font-size: 14px;">
                                <a href="mailto:support@originalinvoice.com" style="color: #22c55e; text-decoration: none;">support@originalinvoice.com</a>
                            </p>
                        </div>
                        
                        <p style="color: #94a3b8; font-size: 12px; margin: 16px 0 0 0;">
                            © 2025 Original Invoice. All rights reserved.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(invoiceNumber, senderName, customerName, invoiceNumber, invoiceNumber, customerName);
    }

    @Override
    public void sendPaymentReceiptEmail(String toEmail, String customerName, String receiptNumber, String receiptDate, String invoiceNumber, String invoiceIssueDate, String itemsJson, String subtotal, String vat, String totalAmount, String paymentMethod, String paymentDate, String confirmedBy) {
        String subject = "Payment Receipt - " + receiptNumber;
        String htmlContent = buildPaymentReceiptEmailBody(customerName, toEmail, receiptNumber, receiptDate, invoiceNumber, invoiceIssueDate, itemsJson, subtotal, vat, totalAmount, paymentMethod, paymentDate, confirmedBy);

        try {
            sendEmailInternal(customerName, toEmail, subject, htmlContent);
            log.info("Payment receipt email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send payment receipt email to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send payment receipt email", e);
        }
    }

    private String buildPaymentReceiptEmailBody(String customerName, String customerEmail, String receiptNumber, String receiptDate, String invoiceNumber, String invoiceIssueDate, String itemsJson, String subtotal, String vat, String totalAmount, String paymentMethod, String paymentDate, String confirmedBy) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Payment Receipt</title>
        </head>
        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5; padding: 40px 20px;">
            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                
                <!-- Header -->
                <tr>
                    <td style="padding: 40px 40px 20px 40px; text-align: center; border-bottom: 2px dashed #e0e0e0;">
                        <h1 style="color: #2c3e50; margin: 0 0 20px 0; font-size: 28px; font-weight: 700;">Payment Receipt</h1>
                    </td>
                </tr>
                
                <!-- Logo Section -->
                <tr>
                    <td style="padding: 30px 40px; text-align: center; background-color: #f8f9fa;">
                        <div style="background-color: #e3f2fd; padding: 20px; border-radius: 8px; display: inline-block;">
                            <span style="color: #1976d2; font-size: 24px; font-weight: 700; letter-spacing: 2px;">LOGO</span>
                        </div>
                    </td>
                </tr>
                
                <!-- Receipt Details -->
                <tr>
                    <td style="padding: 30px 40px;">
                        <table cellpadding="0" cellspacing="0" border="0" width="100%%">
                            <tr>
                                <td style="width: 50%%; padding-bottom: 15px;">
                                    <p style="color: #7f8c8d; font-size: 13px; margin: 0 0 5px 0;">Receipt Number:</p>
                                    <p style="color: #2c3e50; font-size: 15px; font-weight: 600; margin: 0;">%s</p>
                                </td>
                                <td style="width: 50%%; padding-bottom: 15px; text-align: right;">
                                    <p style="color: #7f8c8d; font-size: 13px; margin: 0 0 5px 0;">Invoice Number:</p>
                                    <p style="color: #2c3e50; font-size: 15px; font-weight: 600; margin: 0;">%s</p>
                                </td>
                            </tr>
                            <tr>
                                <td style="width: 50%%; padding-bottom: 15px;">
                                    <p style="color: #7f8c8d; font-size: 13px; margin: 0 0 5px 0;">Receipt Date:</p>
                                    <p style="color: #2c3e50; font-size: 15px; font-weight: 600; margin: 0;">%s</p>
                                </td>
                                <td style="width: 50%%; padding-bottom: 15px; text-align: right;">
                                    <p style="color: #7f8c8d; font-size: 13px; margin: 0 0 5px 0;">Invoice Issue Date:</p>
                                    <p style="color: #2c3e50; font-size: 15px; font-weight: 600; margin: 0;">%s</p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                
                <!-- Customer Information -->
                <tr>
                    <td style="padding: 0 40px 30px 40px;">
                        <h3 style="color: #2c3e50; font-size: 16px; font-weight: 700; margin: 0 0 15px 0;">Customer Information</h3>
                        <p style="color: #2c3e50; font-size: 15px; font-weight: 600; margin: 0 0 5px 0;">%s</p>
                        <p style="color: #7f8c8d; font-size: 14px; margin: 0;">%s</p>
                    </td>
                </tr>
                
                <!-- Items Table -->
                <tr>
                    <td style="padding: 0 40px 30px 40px;">
                        <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="border-collapse: collapse;">
                            <thead>
                                <tr style="background-color: #f8f9fa;">
                                    <th style="padding: 12px; text-align: left; color: #7f8c8d; font-size: 13px; font-weight: 600; border-bottom: 2px solid #e0e0e0;">#</th>
                                    <th style="padding: 12px; text-align: left; color: #7f8c8d; font-size: 13px; font-weight: 600; border-bottom: 2px solid #e0e0e0;">Item Detail</th>
                                    <th style="padding: 12px; text-align: center; color: #7f8c8d; font-size: 13px; font-weight: 600; border-bottom: 2px solid #e0e0e0;">Qty</th>
                                    <th style="padding: 12px; text-align: right; color: #7f8c8d; font-size: 13px; font-weight: 600; border-bottom: 2px solid #e0e0e0;">Rate</th>
                                    <th style="padding: 12px; text-align: right; color: #7f8c8d; font-size: 13px; font-weight: 600; border-bottom: 2px solid #e0e0e0;">Amount</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                    </td>
                </tr>
                
                <!-- Payment Summary -->
                <tr>
                    <td style="padding: 0 40px 30px 40px;">
                        <h3 style="color: #2c3e50; font-size: 16px; font-weight: 700; margin: 0 0 15px 0;">Payment Summary</h3>
                        <table cellpadding="0" cellspacing="0" border="0" width="100%%">
                            <tr>
                                <td style="padding: 8px 0; color: #7f8c8d; font-size: 14px;">Sub Total</td>
                                <td style="padding: 8px 0; text-align: right; color: #2c3e50; font-size: 14px; font-weight: 600;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; color: #7f8c8d; font-size: 14px;">VAT (7.5%%)</td>
                                <td style="padding: 8px 0; text-align: right; color: #2c3e50; font-size: 14px; font-weight: 600;">%s</td>
                            </tr>
                            <tr style="border-top: 2px solid #e0e0e0;">
                                <td style="padding: 12px 0; color: #2c3e50; font-size: 16px; font-weight: 700;">Total Amount Paid</td>
                                <td style="padding: 12px 0; text-align: right; color: #27ae60; font-size: 18px; font-weight: 700;">%s</td>
                            </tr>
                        </table>
                    </td>
                </tr>
                
                <!-- Payment Confirmation -->
                <tr>
                    <td style="padding: 0 40px 30px 40px;">
                        <h3 style="color: #2c3e50; font-size: 16px; font-weight: 700; margin: 0 0 15px 0;">Payment Confirmation</h3>
                        <table cellpadding="0" cellspacing="0" border="0" width="100%%">
                            <tr>
                                <td style="padding: 8px 0; color: #7f8c8d; font-size: 14px;">Payment Method</td>
                                <td style="padding: 8px 0; text-align: right; color: #2c3e50; font-size: 14px; font-weight: 600;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; color: #7f8c8d; font-size: 14px;">Payment Date</td>
                                <td style="padding: 8px 0; text-align: right; color: #2c3e50; font-size: 14px; font-weight: 600;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; color: #7f8c8d; font-size: 14px;">Confirmed By</td>
                                <td style="padding: 8px 0; text-align: right; color: #2c3e50; font-size: 14px; font-weight: 600;">%s</td>
                            </tr>
                        </table>
                    </td>
                </tr>
                
                <!-- QR Code Placeholder -->
                <tr>
                    <td style="padding: 20px 40px; text-align: center; background-color: #f8f9fa;">
                        <div style="background-color: white; padding: 15px; border-radius: 8px; display: inline-block; border: 2px solid #e0e0e0;">
                            <div style="width: 120px; height: 120px; background-color: #f0f0f0; display: flex; align-items: center; justify-content: center;">
                                <span style="color: #95a5a6; font-size: 12px;">QR Code</span>
                            </div>
                        </div>
                    </td>
                </tr>
                
                <!-- Footer -->
                <tr>
                    <td style="padding: 30px 40px; text-align: center; background-color: #f8f9fa;">
                        <p style="color: #7f8c8d; font-size: 13px; margin: 0 0 10px 0;">
                            This receipt was autogenerated by <span style="color: #3498db; font-weight: 600;">Original Invoice</span>.
                        </p>
                        <p style="color: #7f8c8d; font-size: 13px; margin: 0 0 20px 0;">
                            This document serves as proof of payment.
                        </p>
                        <a href="https://api.originalinvoice.com/receipts/%s/download" style="display: inline-block; background-color: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; font-size: 14px; font-weight: 600;">
                            📄 Download PDF
                        </a>
                    </td>
                </tr>
                
            </table>
        </body>
        </html>
        """.formatted(
            receiptNumber, invoiceNumber,
            receiptDate, invoiceIssueDate,
            customerName, customerEmail,
            itemsJson,
            subtotal, vat, totalAmount,
            paymentMethod, paymentDate, confirmedBy,
            receiptNumber
        );
    }
}
