package invoice.data.repositories;

import invoice.data.models.Invoice;
import invoice.data.constants.Invoice_Status;
import invoice.data.projections.InvoiceStatsProjection;
import invoice.data.projections.PaymentTrendProjection;
import invoice.data.projections.YearlyTrendProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @Query("SELECT i FROM Invoice i WHERE i.invoiceNumber = ?1 AND i.user.id = ?2")
    Optional<Invoice> findByInvoiceNumberAndUserId(String invoiceNumber, UUID userId);

    @Query("SELECT i FROM Invoice i WHERE i.user.id = ?1 ORDER BY i.id DESC")
    List<Invoice> findAllByUserId(UUID userId);

    @Query("SELECT i FROM Invoice i WHERE i.recipient.email = ?1 ORDER BY i.creationDate DESC")
    List<Invoice> findAllByRecipientEmail(String email);

    @Query("SELECT i FROM Invoice i WHERE i.user.id = ?1 ORDER BY i.id DESC LIMIT 1")
    Optional<Invoice> findLastInvoiceByUserId(UUID userId);

    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId ORDER BY i.creationDate DESC")
    List<Invoice> getRecentInvoices(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
        SELECT
            COALESCE(SUM(i.totalDue), 0) AS totalAmount,
            COUNT(i) AS invoiceCount
        FROM Invoice i
        WHERE i.user.id = :userId
    """)
    InvoiceStatsProjection getTotalInvoicesStats(@Param("userId") UUID userId);

    @Query("""
        SELECT
            COALESCE(SUM(i.totalDue), 0) AS totalAmount,
            COUNT(i) AS invoiceCount
        FROM Invoice i
        WHERE i.user.id = :userId
          AND i.status = :status
    """)
    InvoiceStatsProjection getInvoiceStatsByStatus(
            @Param("userId") UUID userId,
            @Param("status") Invoice_Status status
    );

    @Query("""
        SELECT
            COALESCE(SUM(i.totalDue), 0) AS totalAmount,
            COUNT(i) AS invoiceCount
        FROM Invoice i
        WHERE i.user.id = :userId
          AND (i.status = invoice.data.constants.Invoice_Status.OVERDUE
               OR (i.dueDate < :now AND i.status <> invoice.data.constants.Invoice_Status.PAID))
    """)
    InvoiceStatsProjection getOverdueInvoicesStats(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT
            COALESCE(SUM(i.totalDue), 0) AS totalAmount,
            COUNT(i) AS invoiceCount
        FROM Invoice i
        WHERE i.user.id = :userId
          AND i.status = :status
          AND i.creationDate >= :startDate
          AND i.creationDate < :endDate
    """)
    InvoiceStatsProjection getInvoiceStatsByStatusAndDateRange(
            @Param("userId") UUID userId, @Param("status") Invoice_Status status,
            @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT
            YEAR(i.creationDate) AS year,
            MONTH(i.creationDate) AS month,
            COALESCE(SUM(i.totalDue), 0) AS totalAmount,
            COUNT(i) AS invoiceCount
        FROM Invoice i
        WHERE i.user.id = :userId
          AND i.status = invoice.data.constants.Invoice_Status.PAID
          AND i.creationDate >= :startDate
        GROUP BY YEAR(i.creationDate), MONTH(i.creationDate)
        ORDER BY YEAR(i.creationDate), MONTH(i.creationDate)
    """)
    List<PaymentTrendProjection> getPaymentTrendsByMonth(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate);

    @Query("""
        SELECT
            YEAR(i.creationDate) AS year,
            COALESCE(SUM(i.totalDue), 0) AS totalAmount,
            COUNT(i) AS invoiceCount
        FROM Invoice i
        WHERE i.user.id = :userId
          AND i.status = invoice.data.constants.Invoice_Status.PAID
          AND i.creationDate >= :startDate
        GROUP BY YEAR(i.creationDate)
        ORDER BY YEAR(i.creationDate)
    """)
    List<YearlyTrendProjection> getPaymentTrendsByYear(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate);
}