package invoice.security.utils;

import java.util.List;

public class SecurityUtils {

    private SecurityUtils() {}

    public static final String JWT_PREFIX = "Bearer ";

    public static final List<String>
            PUBLIC_ENDPOINTS = List.of(
                "/api/users/register",
                "/api/users/activate",
                "/api/users/get-profile",
                "/api/users/exists",
                "/api/users/isUserValid/**",
                "/api/auth/**",
                "/oauth/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",
                "/v3/api-docs",
                "/swagger-resources/**",
                "/webjars/**"
    );

}
