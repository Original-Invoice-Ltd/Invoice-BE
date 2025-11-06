package invoice.data.models;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String website; //optional field
    private String businessOwner;//optional field
    private String invoiceNumber;
    private String logoUrl;
    private String imageUrl;
    private LocalDate creationDate;
    private LocalDate dueDate;
    private String currency;
    private Double discount;
}
