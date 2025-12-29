package invoice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oauth")
@Data
public class OAuthProperties {
    
    private Google google = new Google();
    private Apple apple = new Apple();
    
    @Data
    public static class Google {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String scope = "openid email profile";
    }
    
    @Data
    public static class Apple {
        private String clientId;
        private String teamId;
        private String keyId;
        private String privateKeyPath;
        private String redirectUri;
    }
}