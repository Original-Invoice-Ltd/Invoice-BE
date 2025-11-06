package invoice.security.services;



import invoice.data.models.User;
import invoice.data.repositories.UserRepository;
import invoice.data.repositories.UserSessionRepository;
import invoice.security.data.models.BlacklistedToken;
import invoice.security.data.repositories.BlacklistedTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    @Autowired
    public AuthServiceImpl(BlacklistedTokenRepository blacklistedTokenRepository, UserSessionRepository userSessionRepository, UserRepository userRepository) {
        this.blacklistedTokenRepository = blacklistedTokenRepository;
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
    }


    @Override
    public void invalidatePreviousTokens(String email, String newToken) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (user.getCurrentToken() != null && !user.getCurrentToken().isEmpty()) {
            blacklist(user.getCurrentToken());
        }
        userSessionRepository.updateCurrentToken(user.getId(), newToken);
    }



    @Override
    public void blacklist(String token) {
        log.info("Trying to blacklist token: {}", token);
        trackExpiredTokens();
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setExpiresAt(now().plus(24, HOURS));
        blacklistedTokenRepository.save(blacklistedToken);
        log.info("Blacklisted token: {}", token);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        log.info("Checking blacklist status of token: {}", token);
        boolean isBlacklisted = blacklistedTokenRepository.existsByToken(token);
        log.info("Blacklist status of token: {}", isBlacklisted);
        trackExpiredTokens();
        return isBlacklisted;
    }

    private void trackExpiredTokens() {
        log.info("Tracking and deleting expired user tokens");
        var blacklist = blacklistedTokenRepository.findAll();
        blacklist.stream()
                .filter(blacklistedToken -> now().isAfter(blacklistedToken.getExpiresAt()))
                .forEach(blacklistedTokenRepository::delete);
        log.info("Expired user tokens successfully tracked and deleted");
    }

}
