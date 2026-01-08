package invoice.dtos.response;


import invoice.data.models.Invoice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private List<InvoiceItemResponse> items;
    private List<InvoiceTaxResponse> appliedTaxes; // Invoice-level taxes
    private Double subtotal;
    private Double totalTaxAmount;
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
        this.logoUrl = invoice.getLogoUrl() != null ? invoice.getLogoUrl() : "";
        this.signatureUrl = invoice.getSignatureUrl() != null ? invoice.getSignatureUrl() : "";
        
        // Safe date mapping
        this.creationDate = invoice.getCreationDate() != null ? invoice.getCreationDate().toLocalDate() : null;
        this.dueDate = invoice.getDueDate() != null ? invoice.getDueDate().toLocalDate() : null;
        
        // Safe enum mapping
        this.status = invoice.getStatus() != null ? invoice.getStatus().toString() : null;
        this.currency = invoice.getCurrency();
        
        // Safe mapping of invoice items
        this.items = invoice.getItems() != null ? 
            invoice.getItems().stream()
                .map(InvoiceItemResponse::new)
                .collect(Collectors.toList()) : 
            List.of();
        
        // Safe mapping of invoice-level taxes
        this.appliedTaxes = invoice.getInvoiceTaxes() != null ? 
            invoice.getInvoiceTaxes().stream()
                .map(InvoiceTaxResponse::new)
                .collect(Collectors.toList()) : 
            List.of();
        
        // Safe mapping of totals - use calculated values to ensure accuracy
        this.subtotal = invoice.getSubtotal() != null ? invoice.getSubtotal() : 
                       (invoice.getItems() != null ? invoice.calculateSubtotal() : 0.0);
        this.totalTaxAmount = invoice.getTotalTaxAmount() != null ? invoice.getTotalTaxAmount() : 
                             (invoice.getInvoiceTaxes() != null ? invoice.calculateTotalTaxAmount() : 0.0);
        this.totalDue = invoice.getTotalDue() != null ? invoice.getTotalDue() : 
                       (invoice.getItems() != null ? invoice.calculateTotalDue() : 0.0);
        
        // Safe mapping of optional fields
        this.note = invoice.getNote() != null ? invoice.getNote() : "";
        this.termsAndConditions = invoice.getTermsAndConditions() != null ? invoice.getTermsAndConditions() : "";
        this.paymentTerms = invoice.getPaymentTerms() != null ? invoice.getPaymentTerms() : "";
        this.accountNumber = invoice.getAccountNumber() != null ? invoice.getAccountNumber() : "";
        this.accountName = invoice.getAccountName() != null ? invoice.getAccountName() : "";
        this.bank = invoice.getBank() != null ? invoice.getBank() : "";
        
        // Safe mapping of sender and recipient
        this.billFrom = invoice.getSender() != null ? new InvoiceSenderResponse(invoice.getSender()) : null;
        // Note: billTo requires Client entity - this should be set separately if needed
    }
}
