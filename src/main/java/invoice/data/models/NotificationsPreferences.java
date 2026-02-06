package invoice.data.models;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationsPreferences {
    @Builder.Default
    private boolean paymentNotificationsEnabled =true;
    @Builder.Default
    private boolean invoiceNotificationsEnabled =true;
    @Builder.Default
    private boolean invoiceReminderNotificationsEnabled = true;
    @Builder.Default
    private boolean clientNotificationsEnabled =true;
    @Builder.Default
    private boolean systemNotificationsEnabled =true;
}