package invoice.services;

import invoice.dtos.oauth.AppleTokenResponse;
import invoice.dtos.oauth.AppleUserInfo;
import invoice.dtos.oauth.GoogleTokenResponse;
import invoice.dtos.oauth.GoogleUserInfo;

public interface OAuthService {
    String generateGoogleAuthUrl();
    GoogleTokenResponse exchangeGoogleCode(String code);
    GoogleUserInfo getGoogleUserInfo(String accessToken);
    
    String generateAppleAuthUrl();
    AppleTokenResponse exchangeAppleCode(String code);
    AppleUserInfo validateAppleIdToken(String idToken);
    String generateAppleClientSecret();
}