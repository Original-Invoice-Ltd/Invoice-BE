package invoice.data.projections;

public interface InvoiceStatsProjection {
    Double getTotalAmount();
    Long getInvoiceCount();
}