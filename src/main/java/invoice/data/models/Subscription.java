package invoice.data.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;
    
    @Column(name = "paystack_subscription_code")
    private String paystackSubscriptionCode;
    
    @Column(name = "paystack_customer_code")
    private String paystackCustomerCode;
    
    @Column(name = "paystack_email_token")
    private String paystackEmailToken;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
    
    @Column(name = "current_period_start")
    private LocalDateTime currentPeriodStart;
    
    @Column(name = "current_period_end")
    private LocalDateTime currentPeriodEnd;
    
    @Column(name = "next_payment_date")
    private LocalDateTime nextPaymentDate;
    
    @Column(name = "invoices_used_this_month")
    private Integer invoicesUsedThisMonth = 0;
    
    @Column(name = "logos_uploaded_this_month")
    private Integer logosUploadedThisMonth = 0;
    
    @Column(name = "last_reset_date")
    private LocalDateTime lastResetDate;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    public enum SubscriptionPlan {
        FREE("Free", 0, 3, false, 0),
        ESSENTIALS("Essentials", 500000, 10, true, 1), // 5000 NGN in kobo
        PREMIUM("Premium", 1000000, -1, true, -1); // 10000 NGN in kobo, -1 means unlimited
        
        private final String displayName;
        private final long amountInKobo;
        private final int invoiceLimit;
        private final boolean sharingEnabled;
        private final int logoLimit;
        
        SubscriptionPlan(String displayName, long amountInKobo, int invoiceLimit, boolean sharingEnabled, int logoLimit) {
            this.displayName = displayName;
            this.amountInKobo = amountInKobo;
            this.invoiceLimit = invoiceLimit;
            this.sharingEnabled = sharingEnabled;
            this.logoLimit = logoLimit;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public long getAmountInKobo() {
            return amountInKobo;
        }
        
        public int getInvoiceLimit() {
            return invoiceLimit;
        }
        
        public boolean isSharingEnabled() {
            return sharingEnabled;
        }
        
        public int getLogoLimit() {
            return logoLimit;
        }
        
        public boolean isUnlimitedInvoices() {
            return invoiceLimit == -1;
        }
        
        public boolean isUnlimitedLogos() {
            return logoLimit == -1;
        }
    }
    
    public enum SubscriptionStatus {
        ACTIVE,
        INACTIVE,
        CANCELLED,
        NON_RENEWING,
        ATTENTION // Payment failed, needs attention
    }
}
