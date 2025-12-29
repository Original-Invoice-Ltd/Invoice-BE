package invoice.data.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import invoice.data.constants.CustomerType;
import invoice.data.constants.TaxType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_taxes")
public class Tax {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;
    
    private String name;
    
    @Enumerated(EnumType.STRING)
    private TaxType taxType; // WHT, VAT, etc.
    
    // Base tax rate (can be overridden by client-specific rates)
    @Column(precision = 5, scale = 2)
    private BigDecimal baseTaxRate;
    
    // Client-specific rates
    @Column(precision = 5, scale = 2)
    private BigDecimal individualRate;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal businessRate;
    
    private String description;
    private boolean isActive = true;

    @Setter(AccessLevel.NONE)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;
    
    @Setter(AccessLevel.NONE)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Get the applicable tax rate based on client type
     */
    public BigDecimal getApplicableRate(CustomerType clientType) {
        if (clientType == null) {
            return baseTaxRate != null ? baseTaxRate : BigDecimal.ZERO;
        }
        
        switch (clientType) {
            case INDIVIDUAL:
                return individualRate != null ? individualRate : 
                       (baseTaxRate != null ? baseTaxRate : BigDecimal.ZERO);
            case BUSINESS:
                return businessRate != null ? businessRate : 
                       (baseTaxRate != null ? baseTaxRate : BigDecimal.ZERO);
            default:
                return baseTaxRate != null ? baseTaxRate : BigDecimal.ZERO;
        }
    }
}
