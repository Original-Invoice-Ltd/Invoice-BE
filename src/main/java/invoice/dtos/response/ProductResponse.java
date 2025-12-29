package invoice.dtos.response;

import invoice.data.models.Product;
import invoice.data.models.Tax;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private UUID id;
    private String itemName;
    private String category;
    private String description;
    private Integer quantity;
    private BigDecimal rate;
    private BigDecimal amount;
    private BigDecimal totalTaxAmount;
    private BigDecimal amountWithTax;
    private List<TaxResponse> taxes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.itemName = product.getItemName();
        this.category = product.getCategory() != null ? product.getCategory().toString() : null;
        this.description = product.getDescription();
        this.quantity = product.getQuantity();
        this.rate = product.getRate();
        this.amount = product.getAmount();
        this.totalTaxAmount = product.getTotalTaxAmount();
        this.amountWithTax = product.getAmountWithTax();
        this.taxes = product.getTaxes().stream()
                .map(TaxResponse::new)
                .collect(Collectors.toList());
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
    }
}