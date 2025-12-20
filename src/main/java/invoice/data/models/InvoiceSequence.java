package invoice.data.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_invoice_sequence", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id"})
})
public class InvoiceSequence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Integer lastSequenceNumber;
    
    public InvoiceSequence(User user, Integer lastSequenceNumber) {
        this.user = user;
        this.lastSequenceNumber = lastSequenceNumber;
    }
}
