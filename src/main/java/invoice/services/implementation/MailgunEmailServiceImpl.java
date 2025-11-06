package invoice.services.implementation;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import invoice.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailgunEmailServiceImpl implements EmailService {

    @Value("${MAILGUN_DOMAIN}")
    private String mailgunDomain;

    @Value("${MAILGUN_SECRET}")
    private String mailgunApiKey;

    @Value("${MAIL_FROM_ADDRESS}")
    private String fromAddress;

    @Value("${MAIL_FROM_NAME}")
    private String fromName;

    @Override
    public void sendWelcomeEmail(String to, String name) {
        try {
            String subject = "Registration Successful!";
            String htmlContent = buildWelcomeEmailBody(name);

            sendEmail(to, subject, htmlContent);
            log.info("Welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    @Override
    public void sendVerificationEmail(String toEmail, String firstName, String verificationToken, String frontendUrl) {
        try {
            log.info("Attempting to send verification email to: {}", toEmail);
            log.info("Frontend URL: {}", frontendUrl);
            log.info("Verification Token: {}", verificationToken);

            String subject = "Verify your email address";
            String verificationUrl = frontendUrl + "?token=" + verificationToken;
            String htmlContent = buildVerificationEmailTemplate(firstName, verificationUrl);

            sendEmail(toEmail, subject, htmlContent);
            log.info("Verification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) throws UnirestException {
        String fromField = fromName + " <" + fromAddress + ">";
        
        log.info("Sending email via Mailgun API to: {} from: {}", to, fromField);
        log.info("Using domain: {} with API key: {}...", mailgunDomain, mailgunApiKey.substring(0, Math.min(10, mailgunApiKey.length())));
        
        // Use asString() instead of asJson() to avoid JSON parsing issues
        HttpResponse<String> request = Unirest.post("https://api.mailgun.net/v3/" + mailgunDomain + "/messages")
                .basicAuth("api", mailgunApiKey)
                .queryString("from", fromField)
                .queryString("to", to)
                .queryString("subject", subject)
                .queryString("html", htmlContent)
                .asString();

        log.info("Mailgun API response status: {} for email to: {}", request.getStatus(), to);
        log.info("Mailgun API response body: {}", request.getBody());

        if (request.getStatus() != 200) {
            log.error("Mailgun API returned status: {} for email to: {}. Response: {}", 
                     request.getStatus(), to, request.getBody());
            throw new RuntimeException("Failed to send email via Mailgun API. Status: " + request.getStatus() + 
                                     ". Response: " + request.getBody());
        }

        log.info("Email sent successfully via Mailgun API to: {} with status: {}", to, request.getStatus());
    }

    private String buildVerificationEmailTemplate(String firstName, String verificationUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><title>Email Verification</title></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #2c3e50;'>Welcome to Original Invoice!</h2>" +
                "<p>Hi " + firstName + ",</p>" +
                "<p>Thank you for registering with us. To complete your registration and activate your account, please click the button below to verify your email address:</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='" + verificationUrl + "' style='background-color: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block;'>Verify Email Address</a>" +
                "</div>" +
                "<p>If the button doesn't work, you can copy and paste the following link into your browser:</p>" +
                "<p style='background-color: #f8f9fa; padding: 10px; border-radius: 5px; word-break: break-all; font-size: 14px;'>" + verificationUrl + "</p>" +
                "<p style='color: #7f8c8d; font-size: 14px;'><strong>Note:</strong> This verification link will expire in 24 hours.</p>" +
                "<p>If you didn't create an account with us, please ignore this email.</p>" +
                "<p>Best regards,<br>The Original Invoice Team</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public void sendOTPEmail(String toEmail, String firstName, String otp) {
        try {
            log.info("Attempting to send OTP email to: {}", toEmail);
            
            String subject = "Your Verification Code";
            String htmlContent = buildOTPEmailTemplate(firstName, otp);

            sendEmail(toEmail, subject, htmlContent);
            log.info("OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    private String buildOTPEmailTemplate(String firstName, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><title>Email Verification Code</title></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #2c3e50;'>Email Verification</h2>" +
                "<p>Hi " + firstName + ",</p>" +
                "<p>To complete your email verification, please use the following 6-digit code:</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<div style='background-color: #f8f9fa; border: 2px solid #3498db; border-radius: 10px; padding: 20px; display: inline-block;'>" +
                "<span style='font-size: 32px; font-weight: bold; color: #2c3e50; letter-spacing: 8px;'>" + otp + "</span>" +
                "</div>" +
                "</div>" +
                "<p style='color: #7f8c8d; font-size: 14px;'><strong>Important:</strong></p>" +
                "<ul style='color: #7f8c8d; font-size: 14px;'>" +
                "<li>This code will expire in 10 minutes</li>" +
                "<li>You have 5 attempts to enter the correct code</li>" +
                "<li>If you didn't request this code, please ignore this email</li>" +
                "</ul>" +
                "<p>If you're having trouble, you can request a new verification code from the app.</p>" +
                "<p>Best regards,<br>The BDIC Team</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public void sendPasswordResetOTPEmail(String toEmail, String firstName, String otp) {
        try {
            log.info("Attempting to send password reset OTP email to: {}", toEmail);
            
            String subject = "Password Reset Code";
            String htmlContent = buildPasswordResetOTPEmailTemplate(firstName, otp);

            sendEmail(toEmail, subject, htmlContent);
            log.info("Password reset OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset OTP email", e);
        }
    }

    private String buildPasswordResetOTPEmailTemplate(String firstName, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'><title>Password Reset Code</title></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #2c3e50;'>Password Reset Request</h2>" +
                "<p>Hi " + firstName + ",</p>" +
                "<p>We received a request to reset your password. Please use the following 6-digit code to proceed:</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<div style='background-color: #fff3cd; border: 2px solid #ffc107; border-radius: 10px; padding: 20px; display: inline-block;'>" +
                "<span style='font-size: 32px; font-weight: bold; color: #856404; letter-spacing: 8px;'>" + otp + "</span>" +
                "</div>" +
                "</div>" +
                "<p style='color: #7f8c8d; font-size: 14px;'><strong>Important:</strong></p>" +
                "<ul style='color: #7f8c8d; font-size: 14px;'>" +
                "<li>This code will expire in 10 minutes</li>" +
                "<li>You have 5 attempts to enter the correct code</li>" +
                "<li>If you didn't request this password reset, please ignore this email</li>" +
                "<li>For security reasons, do not share this code with anyone</li>" +
                "</ul>" +
                "<p>If you're having trouble, you can request a new password reset code from the app.</p>" +
                "<p>Best regards,<br>Original Invoice Team</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private String buildWelcomeEmailBody(String name) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #2c3e50;'>Hello, " + name + "!</h2>" +
                "<p>We are excited to have you onboard at Original Invoice. Get ready to explore amazing features!</p>" +
                "<p>You can now:</p>" +
                "<ul>" +
                "<li>Generate invoices for your clients</li>" +
                "<li>Track payments</li>" +
                "<li>Connect with sellers directly</li>" +
                "</ul>" +
                "<p>If you have any questions, feel free to reach out to our support team.</p>" +
                "<br>" +
                "<p>Best Regards,</p>" +
                "<p><b>Original Invoice Support Team</b></p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}