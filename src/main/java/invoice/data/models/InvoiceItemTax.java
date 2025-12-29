package invoice.data.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_invoice_item_taxes")
public class InvoiceItemTax {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_item_id", nullable = false)
    private InvoiceItem invoiceItem;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tax_id", nullable = false)
    private Tax tax;
    
    // The actual tax rate applied (may differ from base rate based on client type)
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal appliedRate;
    
    // The calculated tax amount for this item
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal taxAmount;
    
    // The base amount on which tax was calculated
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal taxableAmount;
}