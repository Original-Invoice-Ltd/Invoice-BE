package invoice.security.utils;

import java.util.List;

public class SecurityUtils {

    private SecurityUtils() {}

    public static final String JWT_PREFIX = "Bearer ";

    public static final List<String>
            PUBLIC_ENDPOINTS = List.of(
                "/api/users/**",
                "/api/auth/**",
                "/api/v1/users/**",
                "/api/v1/auth/**"
    );

}
