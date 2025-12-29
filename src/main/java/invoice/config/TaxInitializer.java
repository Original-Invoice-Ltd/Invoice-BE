package invoice.config;

import invoice.services.TaxService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class TaxInitializer implements CommandLineRunner {
    
    private final TaxService taxService;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Initializing default taxes...");
            taxService.initializeDefaultTaxes();
            log.info("Tax initialization completed successfully");
        } catch (Exception e) {
            log.error("Error during tax initialization: {}", e.getMessage(), e);
        }
    }
}