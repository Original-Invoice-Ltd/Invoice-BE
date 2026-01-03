package invoice.controllers;

import invoice.config.AppProperties;
import invoice.data.constants.Role;
import invoice.data.constants.UserStatus;
import invoice.data.models.User;
import invoice.data.repositories.UserRepository;
import invoice.dtos.oauth.AppleTokenResponse;
import invoice.dtos.oauth.AppleUserInfo;
import invoice.dtos.oauth.GoogleTokenResponse;
import invoice.dtos.oauth.GoogleUserInfo;
import invoice.exception.BusinessException;
import invoice.security.data.models.SecureUser;
import invoice.services.OAuthService;
import invoice.utiils.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import invoice.security.config.RsaKeyProperties;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MINUTES;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {
    
    private final OAuthService oauthService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final RsaKeyProperties rsaKeys;
    private final HttpServletResponse response;
    
    // Google OAuth endpoints
    @GetMapping("/google/login")
    public void googleLogin(@RequestParam(required = false) String state, HttpServletResponse response) throws IOException {
        String authUrl = oauthService.generateGoogleAuthUrl();
        
        // If state is provided, append it to the auth URL
        if (state != null && !state.isEmpty()) {
            authUrl += "&state=" + java.net.URLEncoder.encode(state, "UTF-8");
        }
        
        response.sendRedirect(authUrl);
    }
    
    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam String code, 
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {
        try {
            // Parse phone number and flow type from state if provided
            String phoneNumber = null;
            boolean isSignIn = false;
            if (state != null && !state.isEmpty()) {
                try {
                    String decodedState = new String(java.util.Base64.getDecoder().decode(state));
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode stateNode = mapper.readTree(decodedState);
                    phoneNumber = stateNode.has("phoneNumber") ? stateNode.get("phoneNumber").asText() : null;
                    isSignIn = stateNode.has("isSignIn") ? stateNode.get("isSignIn").asBoolean() : false;
                    if (phoneNumber != null && phoneNumber.isEmpty()) {
                        phoneNumber = null;
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse OAuth state parameter", e);
                }
            }
            
            // Exchange code for tokens
            GoogleTokenResponse tokenResponse = oauthService.exchangeGoogleCode(code);
            
            // Get user info from Google
            GoogleUserInfo userInfo = oauthService.getGoogleUserInfo(tokenResponse.getAccessToken());
            
            // Check if user exists
            boolean userExists = userRepository.findByEmail(userInfo.getEmail()).isPresent();
            
            // For sign-in flow, only use phone number if user doesn't exist (new user)
            if (isSignIn && userExists) {
                phoneNumber = null; // Don't update phone for existing users during sign-in
            }
            
            // Create or find user with phone number
            User user = createOrUpdateUser(
                userInfo.getEmail(),
                userInfo.getName(),
                userInfo.getGivenName(),
                userInfo.getFamilyName(),
                "GOOGLE",
                userInfo.getId(),
                tokenResponse.getRefreshToken(),
                phoneNumber
            );
            
            // Generate JWT tokens
            Authentication authentication = createAuthentication(user);
            String accessToken = generateAccessToken(authentication);
            String refreshToken = generateRefreshToken(authentication);
            
            // Update user's current token for session validation
            user.setCurrentToken(accessToken);
            userRepository.save(user);
            
            // Set cookies
            response.addCookie(CookieUtils.createAccessTokenCookie(accessToken));
            response.addCookie(CookieUtils.createRefreshTokenCookie(refreshToken));
            
            // Always redirect to dashboard since phone number is collected upfront
            response.sendRedirect(appProperties.getFrontend().getDashboardUrl());
            
        } catch (Exception e) {
            log.error("Google OAuth callback failed", e);
            response.sendRedirect(appProperties.getFrontend().getDashboardUrl().replace("/dashboard/overview", "/signIn?error=oauth_failed"));
        }
    }
    
    // Apple OAuth endpoints
    @GetMapping("/apple/login")
    public void appleLogin(@RequestParam(required = false) String state, HttpServletResponse response) throws IOException {
        String authUrl = oauthService.generateAppleAuthUrl();
        
        // If state is provided, append it to the auth URL
        if (state != null && !state.isEmpty()) {
            authUrl += "&state=" + java.net.URLEncoder.encode(state, "UTF-8");
        }
        
        response.sendRedirect(authUrl);
    }
    
    @PostMapping("/apple/callback")
    public void appleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String id_token,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {
        try {
            // Parse phone number and flow type from state if provided
            String phoneNumber = null;
            boolean isSignIn = false;
            if (state != null && !state.isEmpty()) {
                try {
                    String decodedState = new String(java.util.Base64.getDecoder().decode(state));
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode stateNode = mapper.readTree(decodedState);
                    phoneNumber = stateNode.has("phoneNumber") ? stateNode.get("phoneNumber").asText() : null;
                    isSignIn = stateNode.has("isSignIn") ? stateNode.get("isSignIn").asBoolean() : false;
                    if (phoneNumber != null && phoneNumber.isEmpty()) {
                        phoneNumber = null;
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse OAuth state parameter", e);
                }
            }
            
            AppleUserInfo userInfo;
            
            if (id_token != null) {
                // Validate ID token directly
                userInfo = oauthService.validateAppleIdToken(id_token);
            } else {
                // Exchange code for tokens
                AppleTokenResponse tokenResponse = oauthService.exchangeAppleCode(code);
                userInfo = oauthService.validateAppleIdToken(tokenResponse.getIdToken());
            }
            
            // Parse additional user info if provided (first time only)
            String givenName = null;
            String familyName = null;
            String fullName = null;
            
            if (user != null && !user.isEmpty()) {
                try {
                    // Parse JSON user data from Apple
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode userNode = mapper.readTree(user);
                    
                    if (userNode.has("name")) {
                        com.fasterxml.jackson.databind.JsonNode nameNode = userNode.get("name");
                        givenName = nameNode.has("firstName") ? nameNode.get("firstName").asText() : null;
                        familyName = nameNode.has("lastName") ? nameNode.get("lastName").asText() : null;
                        
                        if (givenName != null && familyName != null) {
                            fullName = givenName + " " + familyName;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse Apple user data", e);
                }
            }
            
            // Check if user exists
            boolean userExists = userRepository.findByEmail(userInfo.getEmail()).isPresent();
            
            // For sign-in flow, only use phone number if user doesn't exist (new user)
            if (isSignIn && userExists) {
                phoneNumber = null; // Don't update phone for existing users during sign-in
            }
            
            // Create or find user
            User existingUser = createOrUpdateUser(
                userInfo.getEmail(),
                fullName,
                givenName,
                familyName,
                "APPLE",
                userInfo.getSub(),
                null, // Apple doesn't provide refresh tokens in this flow
                phoneNumber
            );
            
            // Generate JWT tokens
            Authentication authentication = createAuthentication(existingUser);
            String accessToken = generateAccessToken(authentication);
            String refreshToken = generateRefreshToken(authentication);
            
            // Update user's current token for session validation
            existingUser.setCurrentToken(accessToken);
            userRepository.save(existingUser);
            
            // Set cookies
            response.addCookie(CookieUtils.createAccessTokenCookie(accessToken));
            response.addCookie(CookieUtils.createRefreshTokenCookie(refreshToken));
            
            // Always redirect to dashboard since phone number is collected upfront
            response.sendRedirect(appProperties.getFrontend().getDashboardUrl());
            
        } catch (Exception e) {
            log.error("Apple OAuth callback failed", e);
            response.sendRedirect(appProperties.getFrontend().getDashboardUrl().replace("/dashboard/overview", "/signIn?error=oauth_failed"));
        }
    }
    
    private User createOrUpdateUser(String email, String fullName, String givenName, String familyName, String provider, String providerId, String refreshToken, String phoneNumber) {
        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    // Update existing user
                    boolean needsUpdate = false;
                    
                    // Update name if missing
                    if ((existingUser.getFullName() == null || existingUser.getFullName().isEmpty()) && fullName != null) {
                        existingUser.setFullName(fullName);
                        needsUpdate = true;
                    }
                    
                    // Update phone number if provided and missing
                    if ((existingUser.getPhoneNumber() == null || existingUser.getPhoneNumber().isEmpty()) && phoneNumber != null) {
                        existingUser.setPhoneNumber(phoneNumber);
                        needsUpdate = true;
                    }
                    
                    // Update OAuth provider info if missing
                    if (existingUser.getOauthProvider() == null) {
                        existingUser.setOauthProvider(provider);
                        existingUser.setOauthProviderId(providerId);
                        needsUpdate = true;
                    }
                    
                    // Note: currentToken will be set later with JWT access token
                    
                    // Set as verified and update status
                    if (!existingUser.isVerified() || existingUser.getStatus() != UserStatus.VERIFIED) {
                        existingUser.setVerified(true);
                        existingUser.setStatus(UserStatus.VERIFIED);
                        needsUpdate = true;
                    }
                    
                    return needsUpdate ? userRepository.save(existingUser) : existingUser;
                })
                .orElseGet(() -> {
                    // Create new user
                    User newUser = User.builder()
                            .id(UUID.randomUUID())
                            .email(email)
                            .fullName(fullName != null ? fullName : (givenName != null && familyName != null ? givenName + " " + familyName : email))
                            .phoneNumber(phoneNumber) // Set phone number from OAuth flow
                            .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password for OAuth users
                            .roles(Set.of(Role.USER))
                            .isVerified(true)
                            .status(UserStatus.VERIFIED) // Set status to VERIFIED for OAuth users
                            .oauthProvider(provider)
                            .oauthProviderId(providerId)
                            .currentToken(null) // Will be set later with JWT access token
                            .build();
                    
                    return userRepository.save(newUser);
                });
    }
    
    private Authentication createAuthentication(User user) {
        SecureUser secureUser = new SecureUser(user);
        return new UsernamePasswordAuthenticationToken(secureUser, user.getPassword(), secureUser.getAuthorities());
    }
    
    private String generateAccessToken(Authentication authentication) {
        Algorithm algorithm = Algorithm.RSA512(rsaKeys.publicKey(), rsaKeys.privateKey());
        Instant now = Instant.now();
        SecureUser principal = (SecureUser) authentication.getPrincipal();

        return JWT.create()
                .withIssuer("OriginalInvoiceAccessToken")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(30, MINUTES))
                .withSubject(principal.getUsername())
                .withClaim("principal", principal.getUsername())
                .withClaim("credentials", authentication.getCredentials().toString())
                .withArrayClaim("roles", principal.getRoles().stream().map(Enum::name).toArray(String[]::new))
                .withClaim("type", "access")
                .sign(algorithm);
    }
    
    private String generateRefreshToken(Authentication authentication) {
        Algorithm algorithm = Algorithm.RSA512(rsaKeys.publicKey(), rsaKeys.privateKey());
        Instant now = Instant.now();
        SecureUser principal = (SecureUser) authentication.getPrincipal();

        return JWT.create()
                .withIssuer("OriginalInvoiceRefreshToken")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(30, DAYS))
                .withSubject(principal.getUsername())
                .withClaim("principal", principal.getUsername())
                .withClaim("type", "refresh")
                .sign(algorithm);
    }
}