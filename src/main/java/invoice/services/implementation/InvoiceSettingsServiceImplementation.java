package invoice.services.implementation;

import invoice.config.CloudinaryService;
import invoice.data.constants.Language;
import invoice.data.models.*;
import invoice.dtos.request.BusinessProfileDto;
import invoice.dtos.request.LanguageDto;
import invoice.dtos.request.NotificationsDto;
import invoice.dtos.request.ProfileUpdateRequest;
import invoice.dtos.request.TaxSettingsDto;
import invoice.dtos.response.ProfileUpdateResponse;
import invoice.exception.OriginalInvoiceBaseException;
import invoice.services.InvoiceSettingsService;
import invoice.services.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceSettingsServiceImplementation implements InvoiceSettingsService {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    @Override
    public BusinessProfileDto updateBusinessProfile(String email, BusinessProfileDto request) {
        User user = userService.findByEmail(email);
        BusinessProfileSettings profile = user.getSettings().getProfile();
        updateUserBusinessProfile(request, profile);
        user = userService.updateUser(user);
        return mapToDto(user.getSettings().getProfile());
    }

    @Override
    public TaxSettingsDto updateTaxSettings(String email, TaxSettingsDto request) {
        User user = userService.findByEmail(email);
        TaxSettings tax = user.getSettings().getTaxSettings();
        applyTaxUpdates(request, tax);
        tax.setEnablingVAT(request.isEnablingVAT());
        tax.setEnablingWHT(request.isEnablingWHT());
        userService.updateUser(user);
        return mapToDto(tax);
    }

    @Override
    public NotificationsDto updateNotifications(String email, NotificationsDto request) {
        User user = userService.findByEmail(email);
        NotificationsPreferences notif = user.getSettings().getNotificationsPreferences();
        notif.setPaymentNotificationsEnabled(request.isPaymentRecorded());
        notif.setInvoiceNotificationsEnabled(request.isInvoiceSent());
        notif.setInvoiceReminderNotificationsEnabled(request.isInvoiceReminder());
        notif.setClientNotificationsEnabled(request.isClientAdded());
        notif.setSystemNotificationsEnabled(request.isSystemAlerts());

        userService.updateUser(user);
        return mapToDto(notif);
    }

    @Override
    public LanguageDto updateLanguage(String email, LanguageDto request) {
        User user = userService.findByEmail(email);
        if (request.getLanguage() != null)
            user.getSettings().setLanguage(request.getLanguage());
        userService.updateUser(user);
        return new LanguageDto(user.getSettings().getLanguage());
    }

    @Override
    public BusinessProfileDto getUserBusinessProfile(String email) {
        User user = userService.findByEmail(email);
        return mapToDto(user.getSettings().getProfile());
    }

    @Override
    public NotificationsDto getUserNotificationSettings(String email) {
        User user = userService.findByEmail(email);
        return mapToDto(user.getSettings().getNotificationsPreferences());
    }

    @Override
    public TaxSettingsDto getUserTaxSettings(String email) {
        User user = userService.findByEmail(email);
        return mapToDto(user.getSettings().getTaxSettings());
    }

    @Override
    public Language getUserLanguageSettings(String email) {
        User user = userService.findByEmail(email);
        return user.getSettings().getLanguage();
    }

    @Override
    public String uploadLogo(String email, MultipartFile logoFIle) {
        String logoUrl = null;
        try {
            if (logoFIle != null) {
                logoUrl = cloudinaryService.uploadFile(logoFIle);
            }
        } catch (IOException e) {
            throw new OriginalInvoiceBaseException("Failed to upload logo file");
        }
        return logoUrl;
    }

    @Override
    public ProfileUpdateResponse updatePersonalPofile(ProfileUpdateRequest dto, String email) {
        User user = userService.findByEmail(email);
        return updateProfileAndUpload(dto, user);
    }

    @Override
    public ProfileUpdateResponse getPersonalPofile(String email) {
        User user = userService.findByEmail(email);
        String[] parts = user.getFullName().split(" ", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";
        return ProfileUpdateResponse.builder()
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(user.getPhoneNumber())
                .imageUrl(user.getMediaUrl())
                .build();
    }
    private ProfileUpdateResponse updateProfileAndUpload(ProfileUpdateRequest dto, User user) {
        String[] parts = user.getFullName().split(" ", 2);
        String firstName = parts[0];
        String lastName = parts.length > 1 ? parts[1] : "";
        if (dto.getFirstName() != null && !dto.getFirstName().isEmpty()
                && !firstName.equalsIgnoreCase(dto.getFirstName()))
            firstName = dto.getFirstName();
        if (dto.getLastName() != null && !dto.getLastName().isEmpty() && !lastName.equalsIgnoreCase(dto.getLastName()))
            lastName = dto.getLastName();
        user.setFullName(firstName + " " + lastName);
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isEmpty() && !dto.getPhoneNumber().equalsIgnoreCase(user.getPhoneNumber()))
            user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getProfilePicture() != null && !dto.getProfilePicture().isEmpty()) {
            user.setMediaUrl(uploadPicture(dto.getProfilePicture()));
        }
        user = userService.updateUser(user);
        return ProfileUpdateResponse.builder()
                .firstName(firstName)
                .lastName(lastName)
                .imageUrl(user.getMediaUrl())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    private String uploadPicture(MultipartFile file) {
        try {
            return cloudinaryService.uploadFile(file);
        } catch (IOException ex) {
            throw new OriginalInvoiceBaseException("Unable to upload profile picture");
        }
    }

    private TaxSettingsDto mapToDto(TaxSettings tax) {
        return TaxSettingsDto.builder()
                .taxApplied(tax.getTaxApplied())
                .taxId(tax.getTaxId())
                .enablingVAT(tax.isEnablingVAT())
                .enablingWHT(tax.isEnablingWHT())
                .build();
    }

    private void applyTaxUpdates(TaxSettingsDto request, TaxSettings tax) {
        if (request.getTaxApplied() != null)
            tax.setTaxApplied(request.getTaxApplied());
        if (request.getTaxId() != null)
            tax.setTaxId(request.getTaxId());
    }

    private NotificationsDto mapToDto(NotificationsPreferences notificationDto) {
        return NotificationsDto.builder()
                .paymentRecorded(notificationDto.isPaymentNotificationsEnabled())
                .invoiceSent(notificationDto.isInvoiceNotificationsEnabled())
                .invoiceReminder(notificationDto.isInvoiceReminderNotificationsEnabled())
                .clientAdded(notificationDto.isClientNotificationsEnabled())
                .systemAlerts(notificationDto.isSystemNotificationsEnabled())
                .build();
    }

    private void updateUserBusinessProfile(BusinessProfileDto request, BusinessProfileSettings profile) {
        if (request.getBusinessName() != null && !request.getBusinessName().isEmpty()
                && !request.getBusinessName().equalsIgnoreCase(profile.getBusinessName())) {
            profile.setBusinessName(request.getBusinessName());
        }

        if (request.getBusinessFullName() != null && !request.getBusinessFullName().isEmpty()
                && !request.getBusinessFullName().equalsIgnoreCase(profile.getBusinessFullName())) {
            profile.setBusinessFullName(request.getBusinessFullName());
        }

        if (request.getRegisteredBusinessAddress() != null && !request.getRegisteredBusinessAddress().isEmpty()
                && !request.getRegisteredBusinessAddress().equalsIgnoreCase(profile.getRegisteredBusinessAddress())) {
            profile.setRegisteredBusinessAddress(request.getRegisteredBusinessAddress());
        }

        if (request.getEmailAddress() != null && !request.getEmailAddress().isEmpty()
                && !request.getEmailAddress().equalsIgnoreCase(profile.getEmailAddress())) {
            profile.setEmailAddress(request.getEmailAddress());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()
                && !request.getPhoneNumber().equalsIgnoreCase(profile.getPhoneNumber())) {
            profile.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getBusinessType() != null && request.getBusinessType() != profile.getBusinessType()) {
            profile.setBusinessType(request.getBusinessType());
        }

        if (request.getCountry() != null && request.getCountry() != profile.getCountry()) {
            profile.setCountry(request.getCountry());
        }

        if (request.getBusinessRegistrationNumber() != null && !request.getBusinessRegistrationNumber().isEmpty()
                && !request.getBusinessRegistrationNumber().equalsIgnoreCase(profile.getBusinessRegistrationNumber())) {
            profile.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
        }

        if (request.getBusinessLogoUrl() != null && !request.getBusinessLogoUrl().isEmpty()
                && !request.getBusinessLogoUrl().equalsIgnoreCase(profile.getBusinessLogoUrl())) {
            profile.setBusinessLogoUrl(request.getBusinessLogoUrl());
        }
    }

    private BusinessProfileDto mapToDto(BusinessProfileSettings profile) {
        return BusinessProfileDto.builder()
                .businessName(profile.getBusinessName())
                .businessFullName(profile.getBusinessFullName())
                .registeredBusinessAddress(profile.getRegisteredBusinessAddress())
                .emailAddress(profile.getEmailAddress())
                .phoneNumber(profile.getPhoneNumber())
                .businessType(profile.getBusinessType())
                .country(profile.getCountry())
                .businessRegistrationNumber(profile.getBusinessRegistrationNumber())
                .businessLogoUrl(profile.getBusinessLogoUrl())
                .build();
    }

}
