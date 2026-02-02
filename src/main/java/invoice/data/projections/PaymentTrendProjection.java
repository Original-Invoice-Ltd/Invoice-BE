package invoice.data.projections;

public interface PaymentTrendProjection {
    Integer getYear();
    Integer getMonth();
    Double getTotalAmount();
    Long getInvoiceCount();
}