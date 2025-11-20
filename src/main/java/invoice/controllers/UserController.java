package invoice.controllers;


import invoice.data.models.User;
import invoice.data.repositories.UserRepository;
import invoice.dtos.request.SignUpRequest;
import invoice.dtos.response.SignUpResponse;
import invoice.dtos.response.UserResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

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
    public boolean isUserValid(@PathVariable Long userId) {
        return userService.existsById(userId);
    }
    @GetMapping("/exists")
    public boolean isUserValid(@RequestParam String email) {
        return userService.existsByEmail(email); // returns true/false
    }

    @DeleteMapping("/delete-user")
    public ResponseEntity<?>deleteUser(@RequestParam Long id){
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
    public ResponseEntity<?>disableUser(@RequestParam Long id){
        try{
            String response = userService.disableUser(id);
            return new ResponseEntity<>(response, OK);
        }
        catch (OriginalInvoiceBaseException ex){
            return new ResponseEntity<>(ex.getMessage(), BAD_REQUEST);
        }
    }
}
