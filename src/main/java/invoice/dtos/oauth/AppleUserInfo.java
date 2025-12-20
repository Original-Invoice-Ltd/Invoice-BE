package invoice.dtos.oauth;

import lombok.Data;

@Data
public class AppleUserInfo {
    private String sub; // Apple user ID
    private String email;
    private Boolean emailVerified;
    private String name;
    private String givenName;
    private String familyName;
}