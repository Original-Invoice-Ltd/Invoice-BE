package invoice.data.repositories;

import invoice.data.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query("select p from Product p where p.itemName=:itemName and p.user.email=:email")
    Optional<Product> findByItemNameAndUserEmail(String itemName, String email);
    
    @Query("select p from Product p where p.user.email=:email")
    List<Product> findAllByUserEmail(String email);
    
    @Query("select p from Product p where p.id in :ids and p.user.email=:email")
    List<Product> findByIdsAndUserEmail(List<UUID> ids, String email);
}