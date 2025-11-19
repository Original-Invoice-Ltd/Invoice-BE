package invoice.services.implementation;

import invoice.services.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MailgunEmailServiceImplTest {
    @Autowired
    private EmailService emailService;

    @Test
    public void sendWelcomeEmailTest() {
        emailService.sendWelcomeEmail("victormsonter@gmail.com","Aquiba");
    }

}