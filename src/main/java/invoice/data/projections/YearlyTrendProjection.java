package invoice.data.projections;

public interface YearlyTrendProjection {
    Integer getYear();
    Double getTotalAmount();
    Long getInvoiceCount();
}