package invoice.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceItemRequest {
    private Long id; // null for new items
    private String itemName;
    private String category;
    private String description;
    private Integer quantity;
    private BigDecimal rate;
    private BigDecimal amount;
}
