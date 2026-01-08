package invoice.dtos.response;

import invoice.data.models.InvoiceItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceItemResponse {
    private Long id;
    private String itemName;
    private String category;
    private String description;
    private Integer quantity;
    private BigDecimal rate;
    private BigDecimal amount;

    public InvoiceItemResponse(InvoiceItem item) {
        this.id = item.getId();
        this.itemName = item.getItemName();
        this.category = item.getCategory() != null ? item.getCategory().toString() : null;
        this.description = item.getDescription();
        this.quantity = item.getQuantity();
        this.rate = item.getRate();
        this.amount = item.getAmount();
    }
}
