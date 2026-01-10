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
        String viewInvoiceUrl = frontendUrl + "/invoices/" + invoiceId;
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
        String viewInvoiceUrl = frontendUrl + "/invoices/" + invoiceId;
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
                        <h2 style="color: #1e293b; margin: 0 0 24px 0; font-size: 32px; font-weight: 700; line-height: 1.2;">New Invoice Available</h2>
                        
                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin: 0 0 8px 0;">Hi <strong>%s</strong>,</p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 16px 0;">
                            A new invoice has been generated for you.
                        </p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;">
                            Click the button below to view your invoice details and manage your payment.
                        </p>
                        
                        <!-- CTA Button -->
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="%s" style="background: linear-gradient(135deg, #3b82f6 0%%, #1d4ed8 100%%); color: white; padding: 16px 32px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);">
                                View Invoice
                            </a>
                        </div>
                        
                        <p style="color: #64748b; font-size: 14px; line-height: 1.5; margin: 32px 0 0 0;">
                            If you have any questions, contact support.
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
                        <h2 style="color: #1e293b; margin: 0 0 24px 0; font-size: 28px; font-weight: 700; line-height: 1.2;">Invoice #%s sent to %s</h2>
                        
                        <p style="color: #475569; font-size: 16px; line-height: 1.6; margin: 0 0 8px 0;">Hi %s,</p>
                        
                        <p style="color: #64748b; font-size: 16px; line-height: 1.6; margin: 0 0 32px 0;">
                            Your invoice #%s for %s has been successfully sent to %s.
                        </p>
                        
                        <!-- Invoice Amount Box -->
                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 32px; text-align: center; margin: 32px 0;">
                            <p style="color: #64748b; font-size: 14px; margin: 0 0 12px 0; text-transform: uppercase; letter-spacing: 0.5px; font-weight: 600;">INVOICE AMOUNT</p>
                            <h1 style="color: #1e293b; margin: 0; font-size: 36px; font-weight: 800;">%s</h1>
                        </div>
                        
                        <!-- Invoice Details -->
                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 24px; margin: 24px 0;">
                            <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="font-size: 14px;">
                                <tr>
                                    <td style="color: #64748b; padding: 8px 0; width: 40%%; font-weight: 500;">Invoice No</td>
                                    <td style="color: #1e293b; padding: 8px 0; text-align: right; font-weight: 600;">%s</td>
                                </tr>
                                <tr>
                                    <td style="color: #64748b; padding: 8px 0; font-weight: 500;">Invoice Date</td>
                                    <td style="color: #1e293b; padding: 8px 0; text-align: right; font-weight: 600;">%s</td>
                                </tr>
                                <tr>
                                    <td style="color: #64748b; padding: 8px 0; font-weight: 500;">Due Date</td>
                                    <td style="color: #1e293b; padding: 8px 0; text-align: right; font-weight: 600;">%s</td>
                                </tr>
                            </table>
                        </div>
                        
                        <!-- CTA Button -->
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="%s" style="background: linear-gradient(135deg, #3b82f6 0%%, #1d4ed8 100%%); color: white; padding: 16px 32px; text-decoration: none; border-radius: 8px; display: inline-block; font-weight: 600; font-size: 16px; box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);">
                                View Invoice
                            </a>
                        </div>
                        
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
        """.formatted(invoiceNumber, clientName, invoiceNumber, clientName, firstName, invoiceNumber, amount, clientName, amount, invoiceNumber, invoiceDate, dueDate, viewInvoiceUrl);
    }
}
