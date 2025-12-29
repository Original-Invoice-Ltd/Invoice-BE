package invoice.data.models;

import invoice.data.constants.Item_Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_invoice_items")
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    
    private String itemName;
    
    @Enumerated(EnumType.STRING)
    private Item_Category category;
    
    private String description;
    private Integer quantity;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal rate;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    
    // Legacy tax field - kept for backward compatibility
    @Column(precision = 15, scale = 2)
    private BigDecimal tax;
    
    // New tax structure - one-to-many relationship with calculated tax amounts
    @OneToMany(mappedBy = "invoiceItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<InvoiceItemTax> itemTaxes = new ArrayList<>();
    
    // Helper methods to manage bidirectional relationship
    public void addItemTax(InvoiceItemTax itemTax) {
        itemTaxes.add(itemTax);
        itemTax.setInvoiceItem(this);
    }
    
    public void removeItemTax(InvoiceItemTax itemTax) {
        itemTaxes.remove(itemTax);
        itemTax.setInvoiceItem(null);
    }
    
    /**
     * Calculate total tax amount for this item
     */
    public BigDecimal getTotalTaxAmount() {
        return itemTaxes.stream()
                .map(InvoiceItemTax::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Get the final amount including all taxes
     */
    public BigDecimal getAmountWithTax() {
        BigDecimal baseAmount = amount != null ? amount : BigDecimal.ZERO;
        return baseAmount.add(getTotalTaxAmount());
    }
}
