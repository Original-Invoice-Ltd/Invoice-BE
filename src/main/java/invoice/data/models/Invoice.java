package invoice.data.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import invoice.data.constants.Invoice_Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    private String title;
    private String businessOwner; // optional
    private String invoiceNumber;
    private String invoiceColor;
    private String logoUrl;
    private String signatureUrl; // optional - URL to signature image
    private UUID clientId;
    
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "sender_id")
    private InvoiceSender sender;
    
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "recipient_id")
    private InvoiceRecipient recipient;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creationDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;
    
    private String paymentTerms;
    private String accountNumber;
    private String accountName;
    private String bank;
    @Enumerated(EnumType.STRING)
    private Invoice_Status status;
    private String currency;
    private Double discount;
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItem> items = new ArrayList<>();
    
    private Double subtotal;
    private Double totalTaxAmount; // Total of all taxes applied
    private Double totalDue;
    private String note;
    private String termsAndConditions;
    
    // Helper methods to manage bidirectional relationship
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }
    
    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
    }
    
    /**
     * Calculate total tax amount for all items in this invoice
     */
    public Double calculateTotalTaxAmount() {
        return items.stream()
                .mapToDouble(item -> item.getTotalTaxAmount().doubleValue())
                .sum();
    }
    
    /**
     * Calculate subtotal (sum of all item amounts without tax)
     */
    public Double calculateSubtotal() {
        return items.stream()
                .mapToDouble(item -> item.getAmount() != null ? item.getAmount().doubleValue() : 0.0)
                .sum();
    }
    
    /**
     * Calculate total due (subtotal + taxes - discount)
     */
    public Double calculateTotalDue() {
        double calculatedSubtotal = calculateSubtotal();
        double calculatedTaxAmount = calculateTotalTaxAmount();
        double discountAmount = discount != null ? discount : 0.0;
        return calculatedSubtotal + calculatedTaxAmount - discountAmount;
    }
}
