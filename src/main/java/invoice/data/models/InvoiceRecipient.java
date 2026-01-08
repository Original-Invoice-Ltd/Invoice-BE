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
@Table(name = "_invoice_recipients")
public class InvoiceRecipient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String businessName;
    private String email;
    private String phone;
    private String address;
    private String customerType;
    private String title;
    private String country;
    private String businessNumber;
    private String fax;
}
