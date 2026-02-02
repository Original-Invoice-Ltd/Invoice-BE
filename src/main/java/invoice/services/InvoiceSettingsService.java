package invoice.services;

import invoice.data.constants.Language;
import invoice.dtos.request.BusinessProfileDto;
import invoice.dtos.request.LanguageDto;
import invoice.dtos.request.NotificationsDto;
import invoice.dtos.request.ProfileUpdateRequest;
import invoice.dtos.request.TaxSettingsDto;
import invoice.dtos.response.ProfileUpdateResponse;

import org.springframework.web.multipart.MultipartFile;

public interface InvoiceSettingsService {
    BusinessProfileDto updateBusinessProfile(String email, BusinessProfileDto request);
    TaxSettingsDto updateTaxSettings(String email, TaxSettingsDto request);

    NotificationsDto updateNotifications(String email, NotificationsDto request);

    LanguageDto updateLanguage(String email, LanguageDto request);

    BusinessProfileDto getUserBusinessProfile(String email);

    NotificationsDto getUserNotificationSettings(String email);

    TaxSettingsDto getUserTaxSettings(String email);

    Language getUserLanguageSettings(String email);

    String uploadLogo(String email, MultipartFile logoFIle);

    ProfileUpdateResponse updatePersonalPofile(ProfileUpdateRequest dto, String email);
    
    ProfileUpdateResponse getPersonalPofile(String email);
}
