package invoice.security.services;

import invoice.data.models.User;

public interface JwtService {
    String generateToken(String email, String password, String[] roles);
    String generateTokenForUser(User user);
}