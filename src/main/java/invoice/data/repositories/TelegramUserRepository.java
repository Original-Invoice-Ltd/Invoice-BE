package invoice.data.repositories;

import invoice.data.models.TelegramUser;
import invoice.data.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TelegramUserRepository extends JpaRepository<TelegramUser, UUID> {

    Optional<TelegramUser> findByUser(User user);

    Optional<TelegramUser> findByChatId(Long chatId);
    Optional<TelegramUser> findByUsernameIgnoreCase(String username);
}