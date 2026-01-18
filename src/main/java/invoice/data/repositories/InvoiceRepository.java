package invoice.data.repositories;

import invoice.data.models.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    
    @Query("SELECT i FROM Invoice i WHERE i.invoiceNumber = ?1 AND i.user.id = ?2")
    Optional<Invoice> findByInvoiceNumberAndUserId(String invoiceNumber, UUID userId);
    
    @Query("SELECT i FROM Invoice i WHERE i.user.id = ?1 ORDER BY i.id DESC")
    List<Invoice> findAllByUserId(UUID userId);
    
    @Query("SELECT i FROM Invoice i WHERE i.recipient.email = ?1 ORDER BY i.creationDate DESC")
    List<Invoice> findAllByRecipientEmail(String email);
    
    @Query("SELECT i FROM Invoice i WHERE i.user.id = ?1 ORDER BY i.id DESC LIMIT 1")
    Optional<Invoice> findLastInvoiceByUserId(Long userId);
}
