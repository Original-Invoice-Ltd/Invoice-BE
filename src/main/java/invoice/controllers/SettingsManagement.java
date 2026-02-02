package invoice.controllers;

import invoice.data.constants.Language;
import invoice.dtos.request.BusinessProfileDto;
import invoice.dtos.request.LanguageDto;
import invoice.dtos.request.NotificationsDto;
import invoice.dtos.request.ProfileUpdateRequest;
import invoice.dtos.request.TaxSettingsDto;
import invoice.dtos.response.ProfileUpdateResponse;
import invoice.services.InvoiceSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsManagement {
    private final InvoiceSettingsService settingsService;
    @GetMapping("/businessProfile")
    public ResponseEntity<?> getBusinessProfile(Principal principal){
        String email = principal.getName();
        BusinessProfileDto response = settingsService.getUserBusinessProfile(email);
        return ResponseEntity.ok(Map.of(
                "isSuccessful", true,
                "data", response
        ));
    }
    @PatchMapping("/businessProfile")
    public ResponseEntity<?> updateBusinessProfile(Principal principal, @RequestBody BusinessProfileDto request) {
            String email = principal.getName();
            request = settingsService.updateBusinessProfile(email, request);
            return ResponseEntity.status(HttpStatus.OK).body(
                            Map.of(
                                            "isSuccessful", true,
                                            "data", request));

    }
  
    @PatchMapping("/taxSettings")
    public ResponseEntity<?> updateTaxSettings(Principal principal, @RequestBody TaxSettingsDto request){
            String email = principal.getName();
            TaxSettingsDto response = settingsService.updateTaxSettings(email, request);
            return ResponseEntity.ok(Map.of(
                    "isSuccessful", true,
                    "data", response
            ));

    }

    @PatchMapping("/notifications")
    public ResponseEntity<?> updateNotifications(Principal principal, @RequestBody NotificationsDto request){
            String email = principal.getName();
            NotificationsDto response = settingsService.updateNotifications(email, request);
            return ResponseEntity.ok(Map.of(
                    "isSuccessful", true,
                    "data", response
            ));

    }

    @PatchMapping("/language")
    public ResponseEntity<?> updateLanguage(Principal principal,  @RequestBody LanguageDto request){
            String email = principal.getName();
            LanguageDto response = settingsService.updateLanguage(email, request);
            return ResponseEntity.ok(Map.of(
                    "isSuccessful", true,
                    "data", response
            ));

    }


    @GetMapping("/taxSettings")
    public ResponseEntity<?> getTaxSettings(Principal principal) {
        String email = principal.getName();
        TaxSettingsDto response = settingsService.getUserTaxSettings(email);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                "isSuccessful", true,
                "data", response
        ));
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotificationsSettings(Principal principal) {
        String email = principal.getName();
        NotificationsDto response = settingsService.getUserNotificationSettings(email);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                "isSuccessful", true,
                "data", response
        ));
    }

    @GetMapping("/language")
    public ResponseEntity<?> getLanguageSettings(Principal principal) {
            String email = principal.getName();
            Language response = settingsService.getUserLanguageSettings(email);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                            "isSuccessful", true,
                            "data", response));

    }

    @PostMapping(value="/uploadLogo", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadBusinessLogo(Principal principal, @RequestParam("logoFile") MultipartFile logoFIle) {
            String email = principal.getName();
            if (logoFIle == null || logoFIle.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("isSuccessful", false, "data", "file is empty"));
            }
            String logoURL = settingsService.uploadLogo(email, logoFIle);
            return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                            "isSuccessful", true,
                            "data", Map.of("uploadedLogoUrl", logoURL)));
    }
    
    @PatchMapping(value = "/personalProfile",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePersonalProfile(Principal principal, @ModelAttribute ProfileUpdateRequest dto) {
        String email = principal.getName();
        ProfileUpdateResponse response = settingsService.updatePersonalPofile(dto, email);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("isSuccessful", true, "data", response));
    }
    
    @GetMapping("/personalProfile")
    public ResponseEntity<?> fetchPersonalProfile(Principal principal) {
        String email = principal.getName();
        ProfileUpdateResponse dto = settingsService.getPersonalPofile(email);
        return ResponseEntity.status(HttpStatus.OK).body(
            Map.of("isSuccessful",true, "data", dto)
        );
    }
    
}
