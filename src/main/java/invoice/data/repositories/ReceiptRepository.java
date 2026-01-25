package invoice.data.repositories;

import invoice.data.models.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {
    
    Optional<Receipt> findByReceiptNumber(String receiptNumber);
    
    @Query("SELECT r FROM Receipt r WHERE r.invoice.id = :invoiceId")
    Optional<Receipt> findByInvoiceId(@Param("invoiceId") UUID invoiceId);
    
    @Query("SELECT r FROM Receipt r JOIN FETCH r.invoice WHERE r.id = :receiptId")
    Optional<Receipt> findByIdWithInvoice(@Param("receiptId") UUID receiptId);
}
