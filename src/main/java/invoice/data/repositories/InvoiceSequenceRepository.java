package invoice.data.repositories;

import invoice.data.models.InvoiceSequence;
import invoice.data.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InvoiceSequence i WHERE i.user.id = :userId")
    Optional<InvoiceSequence> findByUserIdForUpdate(UUID userId);
    
    Optional<InvoiceSequence> findByUser(User user);
}
