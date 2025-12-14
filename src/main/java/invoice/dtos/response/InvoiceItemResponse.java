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
    private BigDecimal tax; // Legacy field for backward compatibility
    private BigDecimal totalTaxAmount; // Total of all applied taxes
    private BigDecimal amountWithTax; // Amount including all taxes
    private List<ItemTaxResponse> appliedTaxes; // Detailed tax breakdown

    public InvoiceItemResponse(InvoiceItem item) {
        this.id = item.getId();
        this.itemName = item.getItemName();
        this.category = item.getCategory() != null ? item.getCategory().toString() : null;
        this.description = item.getDescription();
        this.quantity = item.getQuantity();
        this.rate = item.getRate();
        this.amount = item.getAmount();
        this.tax = item.getTax(); // Legacy field
        this.totalTaxAmount = item.getTotalTaxAmount();
        this.amountWithTax = item.getAmountWithTax();
        this.appliedTaxes = item.getItemTaxes() != null ? 
            item.getItemTaxes().stream()
                .map(ItemTaxResponse::new)
                .collect(Collectors.toList()) : 
            List.of();
    }
}
