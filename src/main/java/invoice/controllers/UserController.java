package invoice.controllers;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import invoice.data.models.User;
import invoice.data.repositories.UserRepository;
import invoice.dtos.request.SignUpRequest;
import invoice.dtos.response.SignUpResponse;
import invoice.dtos.response.UserResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.security.config.RsaKeyProperties;
import invoice.services.UserService;
import invoice.utiils.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final RsaKeyProperties rsaKeys;
    private final HttpServletResponse response;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignUpRequest signUpRequest) {
        try{
            SignUpResponse signUpResponse = userService.register(signUpRequest);
            
            // After successful registration, generate tokens and set cookies
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            signUpRequest.getEmail().toLowerCase(),
                            signUpRequest.getPassword()
                    )
            );
            
            // Generate access token (15 minutes)
            String accessToken = generateAccessToken(authentication);
            
            // Generate refresh token (30 days)
            String refreshToken = generateRefreshToken(authentication);
            
            // Set tokens as HTTP-only secure cookies
            response.addCookie(CookieUtils.createAccessTokenCookie(accessToken));
            response.addCookie(CookieUtils.createRefreshTokenCookie(refreshToken));
            
            return new ResponseEntity<>(signUpResponse, HttpStatus.CREATED);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    private String generateAccessToken(Authentication authentication) {
        Algorithm algorithm = Algorithm.RSA512(rsaKeys.publicKey(), rsaKeys.privateKey());
        Instant now = Instant.now();
        UserDetails principal = (UserDetails) authentication.getPrincipal();

        return JWT.create()
                .withIssuer("OriginalInvoiceAccessToken")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(15, MINUTES))
                .withSubject(principal.getUsername())
                .withClaim("principal", principal.getUsername())
                .withClaim("credentials", authentication.getCredentials().toString())
                .withArrayClaim("roles", extractAuthorities(authentication.getAuthorities()))
                .withClaim("type", "access")
                .sign(algorithm);
    }
    
    private String generateRefreshToken(Authentication authentication) {
        Algorithm algorithm = Algorithm.RSA512(rsaKeys.publicKey(), rsaKeys.privateKey());
        Instant now = Instant.now();
        UserDetails principal = (UserDetails) authentication.getPrincipal();

        return JWT.create()
                .withIssuer("OriginalInvoiceRefreshToken")
                .withIssuedAt(now)
                .withExpiresAt(now.plus(30, java.time.temporal.ChronoUnit.DAYS))
                .withSubject(principal.getUsername())
                .withClaim("principal", principal.getUsername())
                .withClaim("type", "refresh")
                .sign(algorithm);
    }
    
    private String[] extractAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toArray(String[]::new);
    }

    @PutMapping(value = "/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhoto(@RequestParam String email, MultipartFile image){
        try{
            String response = userService.uploadPhoto(email,image);
            return new ResponseEntity<>(response, OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }


    @PostMapping("/add-admin")
    public ResponseEntity<?>addAdmin(@RequestBody SignUpRequest request) {
        try{
            String response = userService.registerAdmin(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        catch(OriginalInvoiceBaseException e){
            return new ResponseEntity<>(e.getMessage(),BAD_REQUEST);
        }
    }

    @PutMapping("/activate")
    public ResponseEntity<?>activateUser(@RequestParam String email) {
        try{
            String response = userService.activate(email);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        catch(OriginalInvoiceBaseException e){
            return new ResponseEntity<>(e.getMessage(),BAD_REQUEST);
        }
    }


    @GetMapping("/get-profile")
    public ResponseEntity<?>getUserProfile(@RequestParam String email){
        try{
            UserResponse response = userService.getProfileFor(email);
            return ResponseEntity.ok(response);
        }
        catch (OriginalInvoiceBaseException e){
            return new ResponseEntity<>(e.getMessage(),BAD_REQUEST);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(){
        List<User> users = userRepository.findAll();
        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::new)
                .toList();
        return new ResponseEntity<>(userResponses, OK);
    }
    @GetMapping("/allCount")
    public ResponseEntity<?> getAllUsersCount(){
        List<User>users = userRepository.findAll();
        return ResponseEntity.ok(users.size());
    }

    @GetMapping("/isUserValid/{userId}")
    public boolean isUserValid(@PathVariable UUID userId) {
        return userService.existsById(userId);
    }
    @GetMapping("/exists")
    public boolean isUserValid(@RequestParam String email) {
        return userService.existsByEmail(email); // returns true/false
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<?>deleteUser(@RequestParam UUID id){
        try{
            String response = userService.deleteUser(id);
            return new ResponseEntity<>(response, OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @DeleteMapping("/delete-by-email")
    public ResponseEntity<?>deleteUser(@RequestParam String email){
        try{
            String response = userService.deleteUserByEmail(email);
            return new ResponseEntity<>(response, OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }

    @PutMapping("/disable-user")
    public ResponseEntity<?>disableUser(@RequestParam UUID id){
        try{
            String response = userService.disableUser(id);
            return new ResponseEntity<>(response, OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }
}
