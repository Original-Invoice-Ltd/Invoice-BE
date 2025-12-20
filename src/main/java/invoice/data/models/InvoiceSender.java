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
@Table(name = "_invoice_senders")
public class InvoiceSender {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String email;
    private String address;//optional
    private String phone;
    private String businessName;
    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;
}
