package invoice.data.repositories;

import invoice.data.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<User, UUID> {


    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.currentToken = :token WHERE u.id = :userId")
    void updateCurrentToken(@Param("userId") UUID userId, @Param("token") String token);
    
    @Query("SELECT u.currentToken FROM User u WHERE u.id = :userId")
    Optional<String> findCurrentTokenByUserId(@Param("userId") UUID userId);
}