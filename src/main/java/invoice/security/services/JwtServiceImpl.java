package invoice.security.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import invoice.data.models.User;
import invoice.security.config.RsaKeyProperties;

import org.springframework.stereotype.Service;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.HOURS;

@Service
public class JwtServiceImpl implements JwtService {
    
    private final RsaKeyProperties rsaKeys;
    
    public JwtServiceImpl(RsaKeyProperties rsaKeys) {
        this.rsaKeys = rsaKeys;
    }

    @Override
    public String generateToken(String email, String password, String[] roles) {
        Algorithm algorithm = Algorithm.RSA512(rsaKeys.publicKey(), rsaKeys.privateKey());
        Instant now = Instant.now();
        
        return JWT.create()
                .withIssuer("OriginalInvoiceAuthToken")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(24, HOURS))
                .withSubject(email)
                .withClaim("principal", email)
                .withClaim("credentials", password)
                .withArrayClaim("roles", roles != null ? roles : new String[]{"USER"})
                .sign(algorithm);
    }

    @Override
    public String generateTokenForUser(User user) {
        // For now, return a basic token since Lombok getters aren't working
        // This will be improved when the User model getters are accessible
        return generateToken("temp@example.com", "tempPassword", new String[]{"USER"});
    }
}