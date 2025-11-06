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
    private String name;
    private String email;
    private String address;
    private String cityAndState;
    private String country;
    private String phone;
    private String businessNumber;
    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;
}
