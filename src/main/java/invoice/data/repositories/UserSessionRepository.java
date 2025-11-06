package invoice.data.repositories;

import invoice.data.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<User, Long> {


    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.currentToken = :token WHERE u.id = :userId")
    void updateCurrentToken(@Param("userId") Long userId, @Param("token") String token);
    
    @Query("SELECT u.currentToken FROM User u WHERE u.id = :userId")
    Optional<String> findCurrentTokenByUserId(@Param("userId") Long userId);
}