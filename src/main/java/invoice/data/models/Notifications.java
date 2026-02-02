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
public class Notifications {
    @Builder.Default
    private boolean wantsPaymentRecorded=false;
    @Builder.Default
    private boolean wantsInvoiceSentNotifications=false;
    @Builder.Default
    private boolean wantsInvoiceReminder=false;
    @Builder.Default
    private boolean wantsClientAdded=false;
    @Builder.Default
    private boolean wantsSystemAlerts=false;
}
