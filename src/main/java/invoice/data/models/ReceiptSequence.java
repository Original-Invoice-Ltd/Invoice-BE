package invoice.data.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_receipt_sequences")
public class ReceiptSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;
    
    @Column(nullable = false)
    private Integer lastSequenceNumber;
    
    public ReceiptSequence(Integer lastSequenceNumber) {
        this.lastSequenceNumber = lastSequenceNumber;
    }
}
