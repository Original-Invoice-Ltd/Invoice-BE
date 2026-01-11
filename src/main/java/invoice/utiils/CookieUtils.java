package invoice.utiils;

import jakarta.servlet.http.Cookie;

public class CookieUtils {
    
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int ACCESS_TOKEN_MAX_AGE = 1800; // 30 minutes
    private static final int REFRESH_TOKEN_MAX_AGE = 2592000; // 30 days
    
    // Check if we're in development mode (localhost)
    private static boolean isSecureEnvironment() {
        String environment = System.getProperty("spring.profiles.active", "dev");
        return !"dev".equals(environment) && !"development".equals(environment);
    }
    
    public static Cookie createAccessTokenCookie(String token) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment()); // Only secure in production
        cookie.setPath("/");
        cookie.setMaxAge(ACCESS_TOKEN_MAX_AGE);
        // Use Lax for better compatibility with OAuth redirects
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
    
    public static Cookie createExtendedAccessTokenCookie(String token) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment()); // Only secure in production
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 hour for extended sessions
        // Use Lax for better compatibility with OAuth redirects
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }
    
    public static Cookie createRefreshTokenCookie(String token) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment()); // Only secure in production
        cookie.setPath("/");
        cookie.setMaxAge(REFRESH_TOKEN_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax"); // Changed from Strict to Lax for OAuth
        return cookie;
    }
    
    public static Cookie deleteAccessTokenCookie() {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }
    
    public static Cookie deleteRefreshTokenCookie() {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment());
        cookie.setPath("/");
        cookie.setMaxAge(0);
        return cookie;
    }
    
    public static Cookie clearAccessTokenCookie() {
        return deleteAccessTokenCookie();
    }
    
    public static Cookie clearRefreshTokenCookie() {
        return deleteRefreshTokenCookie();
    }
}
