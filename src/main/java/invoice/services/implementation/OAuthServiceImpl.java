package invoice.services.implementation;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import invoice.config.OAuthProperties;
import invoice.dtos.oauth.AppleTokenResponse;
import invoice.dtos.oauth.AppleUserInfo;
import invoice.dtos.oauth.GoogleTokenResponse;
import invoice.dtos.oauth.GoogleUserInfo;
import invoice.exception.BusinessException;
import invoice.services.OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthServiceImpl implements OAuthService {
    
    private final OAuthProperties oauthProperties;
    private final WebClient webClient = WebClient.builder().build();
    
    @Override
    public String generateGoogleAuthUrl() {
        return UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", oauthProperties.getGoogle().getClientId())
                .queryParam("redirect_uri", oauthProperties.getGoogle().getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", oauthProperties.getGoogle().getScope())
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();
    }
    
    @Override
    public GoogleTokenResponse exchangeGoogleCode(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", oauthProperties.getGoogle().getClientId());
        formData.add("client_secret", oauthProperties.getGoogle().getClientSecret());
        formData.add("code", code);
        formData.add("grant_type", "authorization_code");
        formData.add("redirect_uri", oauthProperties.getGoogle().getRedirectUri());
        
        try {
            return webClient.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(GoogleTokenResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to exchange Google code for token", e);
            throw new BusinessException("Failed to authenticate with Google");
        }
    }
    
    @Override
    public GoogleUserInfo getGoogleUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(GoogleUserInfo.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to get Google user info", e);
            throw new BusinessException("Failed to get user information from Google");
        }
    }
    
    @Override
    public String generateAppleAuthUrl() {
        return UriComponentsBuilder.fromHttpUrl("https://appleid.apple.com/auth/authorize")
                .queryParam("client_id", oauthProperties.getApple().getClientId())
                .queryParam("redirect_uri", oauthProperties.getApple().getRedirectUri())
                .queryParam("response_type", "code id_token")
                .queryParam("scope", "name email")
                .queryParam("response_mode", "form_post")
                .build()
                .toUriString();
    }
    
    @Override
    public AppleTokenResponse exchangeAppleCode(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", oauthProperties.getApple().getClientId());
        formData.add("client_secret", generateAppleClientSecret());
        formData.add("code", code);
        formData.add("grant_type", "authorization_code");
        formData.add("redirect_uri", oauthProperties.getApple().getRedirectUri());
        
        try {
            return webClient.post()
                    .uri("https://appleid.apple.com/auth/token")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(AppleTokenResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to exchange Apple code for token", e);
            throw new BusinessException("Failed to authenticate with Apple");
        }
    }
    
    @Override
    public AppleUserInfo validateAppleIdToken(String idToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            
            AppleUserInfo userInfo = new AppleUserInfo();
            userInfo.setSub(claimsSet.getSubject());
            userInfo.setEmail(claimsSet.getStringClaim("email"));
            userInfo.setEmailVerified(claimsSet.getBooleanClaim("email_verified"));
            
            // Apple may not always provide name claims
            if (claimsSet.getClaim("name") != null) {
                userInfo.setName(claimsSet.getStringClaim("name"));
            }
            
            return userInfo;
        } catch (Exception e) {
            log.error("Failed to validate Apple ID token", e);
            throw new BusinessException("Invalid Apple ID token");
        }
    }
    
    @Override
    public String generateAppleClientSecret() {
        try {
            // Load private key
            ECPrivateKey privateKey = loadApplePrivateKey();
            
            // Create JWT header
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(oauthProperties.getApple().getKeyId())
                    .build();
            
            // Create JWT payload
            Instant now = Instant.now();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(oauthProperties.getApple().getTeamId())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600))) // 1 hour
                    .audience("https://appleid.apple.com")
                    .subject(oauthProperties.getApple().getClientId())
                    .build();
            
            // Sign JWT
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new ECDSASigner(privateKey);
            signedJWT.sign(signer);
            
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Failed to generate Apple client secret", e);
            throw new BusinessException("Failed to generate Apple client secret");
        }
    }
    
    private ECPrivateKey loadApplePrivateKey() throws Exception {
        String keyPath = oauthProperties.getApple().getPrivateKeyPath();
        String keyContent;
        
        if (keyPath.startsWith("classpath:")) {
            // Load from classpath
            ClassPathResource resource = new ClassPathResource(keyPath.substring(10));
            keyContent = new String(resource.getInputStream().readAllBytes());
        } else {
            // Load from file system
            keyContent = new String(Files.readAllBytes(Paths.get(keyPath)));
        }
        
        // Remove header, footer, and whitespace
        keyContent = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        
        return (ECPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}