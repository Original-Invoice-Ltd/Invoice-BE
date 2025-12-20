package invoice.data.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import invoice.data.constants.Item_Category;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_user_products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    private String itemName;
    
    @Enumerated(EnumType.STRING)
    private Item_Category category;
    
    private String description;
    private Integer quantity;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal rate;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private List<Tax> taxes = new ArrayList<>();
    
    @Setter(AccessLevel.NONE)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
    
    @Setter(AccessLevel.NONE)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate(){
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate(){
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Calculate total tax amount for this product
     */
    public BigDecimal getTotalTaxAmount() {
        return taxes.stream()
                .map(tax -> {
                    if (amount != null && tax.getBaseTaxRate() != null) {
                        return amount.multiply(tax.getBaseTaxRate().divide(BigDecimal.valueOf(100)));
                    }
                    return BigDecimal.ZERO;
                })
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
