package invoice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    
    private Frontend frontend = new Frontend();
    private String paystackSecretKey;
    private String paystackEssentialsPlanCode;
    private String paystackPremiumPlanCode;
    private boolean paystackMockMode = false; // For development when Cloudflare blocks
    
    @Data
    public static class Frontend {
        private String dashboardUrl;
    }
}