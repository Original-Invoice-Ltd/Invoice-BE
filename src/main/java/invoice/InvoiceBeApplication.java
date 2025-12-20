package invoice;

import invoice.security.config.RsaKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RsaKeyProperties.class)
public class InvoiceBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvoiceBeApplication.class, args);
	}

}
