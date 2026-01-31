package invoice.dtos.response;

import invoice.data.models.Receipt;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptResponse {
    private UUID id;
    private String receiptNumber;
    private UUID invoiceId;
    private String invoiceNumber;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private BigDecimal totalPaid;
    private String pdfUrl;
    private LocalDateTime createdAt;
    
    public ReceiptResponse(Receipt receipt) {
        this.id = receipt.getId();
        this.receiptNumber = receipt.getReceiptNumber();
        this.invoiceId = receipt.getInvoice().getId();
        this.invoiceNumber = receipt.getInvoice().getInvoiceNumber();
        this.paymentDate = receipt.getPaymentDate();
        this.paymentMethod = receipt.getPaymentMethod();
        this.totalPaid = receipt.getTotalPaid();
        this.pdfUrl = receipt.getPdfUrl();
        this.createdAt = receipt.getCreatedAt();
    }
}
