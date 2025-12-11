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
    private BigDecimal rate;
    private BigDecimal amount;
    private BigDecimal tax;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "invoice_item_taxes",
        joinColumns = @JoinColumn(name = "invoice_item_id"),
        inverseJoinColumns = @JoinColumn(name = "tax_id")
    )
    private List<Tax> taxes = new ArrayList<>();

}
