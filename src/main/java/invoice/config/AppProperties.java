package invoice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    
    private Frontend frontend = new Frontend();
    
    @Data
    public static class Frontend {
        private String dashboardUrl;
    }
}