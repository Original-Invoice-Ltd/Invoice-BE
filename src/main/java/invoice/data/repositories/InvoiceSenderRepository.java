package invoice.data.repositories;

import invoice.data.models.InvoiceSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceSenderRepository extends JpaRepository<InvoiceSender, Long> {
    @Query("select i from InvoiceSender i where i.invoice.id=:id")
    Optional<InvoiceSender> findByInvoice(UUID id);
}
