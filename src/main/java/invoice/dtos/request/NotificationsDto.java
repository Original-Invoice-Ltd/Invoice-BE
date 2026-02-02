package invoice.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsDto {
    private boolean paymentRecorded;
    private boolean invoiceSent;
    private boolean invoiceReminder;
    private boolean clientAdded;
    private boolean systemAlerts;
}