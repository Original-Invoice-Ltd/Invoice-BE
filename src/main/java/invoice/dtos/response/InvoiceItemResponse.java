package invoice.dtos.response;

import invoice.data.models.InvoiceItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
    private BigDecimal tax;
    private List<TaxResponse> taxes;

    public InvoiceItemResponse(InvoiceItem item) {
        this.id = item.getId();
        this.itemName = item.getItemName();
        this.category = item.getCategory() != null ? item.getCategory().toString() : null;
        this.description = item.getDescription();
        this.quantity = item.getQuantity();
        this.rate = item.getRate();
        this.amount = item.getAmount();
        this.tax = item.getTax();
        this.taxes = item.getTaxes() != null ? 
            item.getTaxes().stream().map(TaxResponse::new).collect(Collectors.toList()) : 
            List.of();
    }
}
