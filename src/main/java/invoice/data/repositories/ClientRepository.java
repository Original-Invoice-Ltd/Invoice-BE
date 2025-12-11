package invoice.data.repositories;

import invoice.data.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ClientRepository extends JpaRepository<Client, UUID> {
    @Query("select c from Client c where c.email=:email")
    Optional<Client> findByEmail(String email);
    @Query("select c from Client c where c.user.email=:email")
    List<Client> findAllUser(String email);
}
