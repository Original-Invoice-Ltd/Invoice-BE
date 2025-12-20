package invoice.data.repositories;

import invoice.data.constants.TaxType;
import invoice.data.models.Tax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaxRepository extends JpaRepository<Tax, UUID> {
    
    List<Tax> findByIsActiveTrue();
    
    List<Tax> findByTaxTypeAndIsActiveTrue(TaxType taxType);
    
    @Query("SELECT t FROM Tax t WHERE t.name ILIKE %:name% AND t.isActive = true")
    List<Tax> findByNameContainingIgnoreCaseAndIsActiveTrue(@Param("name") String name);
}
