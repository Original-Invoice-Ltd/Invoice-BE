package invoice.config;

// Temporarily commented out for testing
/*
import com.pusher.rest.Pusher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PusherConfig {
    
    @Value("${pusher.app-id}")
    private String appId;
    
    @Value("${pusher.key}")
    private String key;
    
    @Value("${pusher.secret}")
    private String secret;
    
    @Value("${pusher.cluster}")
    private String cluster;
    
    @Value("${pusher.encrypted}")
    private boolean encrypted;
    
    @Bean
    public Pusher pusher() {
        Pusher pusher = new Pusher(appId, key, secret);
        pusher.setCluster(cluster);
        pusher.setEncrypted(encrypted);
        return pusher;
    }
}
*/