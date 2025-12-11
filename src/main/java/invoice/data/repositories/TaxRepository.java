package invoice.data.repositories;

import invoice.data.models.Tax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TaxRepository extends JpaRepository<Tax, UUID> {

}
