package invoice.data.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "telegram_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private long chatId;

    @Column(unique = true, nullable= true)
    private String username;
}
