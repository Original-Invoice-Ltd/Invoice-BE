package invoice.data.repositories;

import invoice.data.models.Subscription;
import invoice.data.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    
    Optional<Subscription> findByUser(User user);
    
    Optional<Subscription> findByUserId(UUID userId);
    
    Optional<Subscription> findByPaystackSubscriptionCode(String subscriptionCode);
    
    Optional<Subscription> findByUserAndStatus(User user, Subscription.SubscriptionStatus status);
}
