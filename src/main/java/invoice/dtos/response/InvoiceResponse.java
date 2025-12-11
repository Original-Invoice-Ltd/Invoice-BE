package invoice.dtos.response;


import invoice.data.models.Invoice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceResponse {
    private UUID id;
    private InvoiceSenderResponse billFrom;
    private ClientResponse billTo;
    private String title;
    private String invoiceNumber;
    private String invoiceColor;
    private String logoUrl;
    private String signatureUrl; // optional - URL to signature image
    private LocalDate creationDate;
    private LocalDate dueDate;
    private String status;
    private String currency;
    private List<TaxResponse> taxes;
    private List<InvoiceItemResponse> items;
    private Double subtotal;
    private Double totalDue;
    private String note;
    private String termsAndConditions;
    private String paymentTerms;
    private String accountNumber;
    private String accountName;
    private String bank;

    public InvoiceResponse(Invoice invoice) {
        this.id = invoice.getId();
        this.title = invoice.getTitle();
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.invoiceColor = invoice.getInvoiceColor();
        this.logoUrl = invoice.getLogoUrl() !=null ? invoice.getLogoUrl() : "";
        this.signatureUrl = invoice.getSignatureUrl() != null ? invoice.getSignatureUrl() : "";
        this.creationDate = invoice.getCreationDate().toLocalDate();
        this.dueDate = invoice.getDueDate().toLocalDate();
        this.status = invoice.getStatus().toString();
        this.currency = invoice.getCurrency();
    }
}
