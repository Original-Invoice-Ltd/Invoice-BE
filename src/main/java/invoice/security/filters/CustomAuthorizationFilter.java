package invoice.security.filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import invoice.data.models.User;
import invoice.data.repositories.UserRepository;
import invoice.security.config.RsaKeyProperties;
import invoice.security.services.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static invoice.security.utils.SecurityUtils.JWT_PREFIX;
import static invoice.security.utils.SecurityUtils.PUBLIC_ENDPOINTS;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@AllArgsConstructor
@Slf4j
public class CustomAuthorizationFilter extends OncePerRequestFilter {
    private final RsaKeyProperties rsaKeys;
    private final AuthService authService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.info("Starting authorization");
        String requestPath = request.getRequestURI();
        boolean isRequestPathPublic = PUBLIC_ENDPOINTS.contains(requestPath);
        if (isRequestPathPublic) {
            log.info("Authorization not needed for public endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(JWT_PREFIX)) {
            String token = authorizationHeader.substring(JWT_PREFIX.length()).strip();
            if (isTokenBlacklisted(response, token)) return;
            if (!isAuthorized(token, response)) return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isTokenBlacklisted(HttpServletResponse response, String token) throws IOException {
        if (authService.isTokenBlacklisted(token)) {
            log.warn("Token is blacklisted: {}", token);
            sendErrorResponse(response,"expired token");
            return true;
        }
        return false;
    }

    private boolean isAuthorized(String token, HttpServletResponse response) throws IOException {
        Algorithm algorithm = Algorithm.RSA512(rsaKeys.publicKey(), rsaKeys.privateKey());
        DecodedJWT decodedJWT;
        try {
            JWTVerifier jwtVerifier = JWT.require(algorithm)
                    .withIssuer("OriginalInvoiceAuthToken")
                    .withClaimPresence("roles")
                    .withClaimPresence("principal")
                    .withClaimPresence("credentials")
                    .build();

            decodedJWT = jwtVerifier.verify(token);
        } catch (JWTVerificationException exception) {
            log.error("JWT verification failed: {}", exception.getMessage());
            sendErrorResponse(response,"Expired or invalid token");
            return false;
        }


        List<? extends GrantedAuthority> authorities = decodedJWT.getClaim("roles")
                .asList(SimpleGrantedAuthority.class);
        String principal = decodedJWT.getClaim("principal").asString();
        User user = userRepository.findByEmail(principal)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!token.equals(user.getCurrentToken())) {
            log.warn("Token is not the current active token for user: {}", principal);
            sendErrorResponse(response, "Session expired - logged in from another device");
            return false;
        }
        String credentials = decodedJWT.getClaim("credentials").asString();
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, credentials, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("User authorization succeeded");
        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        response.getWriter().flush();
    }
}
