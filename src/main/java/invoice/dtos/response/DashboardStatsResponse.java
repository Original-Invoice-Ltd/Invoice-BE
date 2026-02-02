package invoice.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsResponse {
    private InvoiceStatsData totalInvoicesSent;
    private InvoiceStatsData paidInvoices;
    private InvoiceStatsData pendingInvoices;
    private InvoiceStatsData overdueInvoices;
    private StatusDistribution statusDistribution;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InvoiceStatsData {
        private Double amount;
        private Long count;
        private String percentageChange; // Format: "+15.50%" or "-3.20%"

        public InvoiceStatsData(Double amount, Long count) {
            this.amount = amount;
            this.count = count;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusDistribution {
        private StatusData paid;
        private StatusData pending;
        private StatusData overdue;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class StatusData {
            private Double amount;
            private Double percentage;
        }
    }
}