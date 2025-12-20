package invoice.security.services;

public interface AuthService {
    void invalidatePreviousTokens(String email, String newToken);

    void blacklist(String token);
    boolean isTokenBlacklisted(String token);
}