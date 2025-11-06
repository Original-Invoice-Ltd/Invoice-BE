package invoice.security.data.repositories;

import invoice.security.data.models.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {
    boolean existsByToken(String token);
    List<BlacklistedToken> findByExpiresAtBefore(Instant expiresAt);
}
