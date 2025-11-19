package invoice.controllers;


import invoice.dtos.request.SignUpRequest;
import invoice.dtos.response.SignUpResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignUpRequest signUpRequest) {
        try{
            SignUpResponse response = userService.register(signUpRequest);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
