package invoice.data.repositories;

import invoice.data.models.ReceiptSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptSequenceRepository extends JpaRepository<ReceiptSequence, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rs FROM ReceiptSequence rs WHERE rs.id = (SELECT MIN(rs2.id) FROM ReceiptSequence rs2)")
    Optional<ReceiptSequence> findGlobalSequenceForUpdate();
}
