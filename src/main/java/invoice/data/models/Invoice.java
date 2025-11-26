package invoice.data.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private String website; // optional
    private String businessOwner; // optional
    private String invoiceNumber;
    private String logoUrl;
    private String imageUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creationDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    private String currency;
    private Double discount;
}


// package invoice.data.models;


// import jakarta.persistence.*;
// import lombok.AllArgsConstructor;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// import java.time.LocalDate;

// @Setter
// @Getter
// @AllArgsConstructor
// @NoArgsConstructor
// @Entity
// @Table(name = "_invoices")
// public class Invoice {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;
//     private String title;
//     private String website; //optional field
//     private String businessOwner;//optional field
//     private String invoiceNumber;
//     private String logoUrl;
//     private String imageUrl;
//     private LocalDate creationDate;
//     private LocalDate dueDate;
//     private String currency;
//     private Double discount;
// }
