package invoice.security.data.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;
import static java.time.LocalDateTime.now;
import static lombok.AccessLevel.NONE;

@Entity
@Table(name = "blacklisted_tokens")
@Getter
@Setter
public class BlacklistedToken {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Column(nullable = false, length = 1000)
    private String token;
    private Instant expiresAt;
    @Setter(NONE)
    private LocalDateTime blacklistedAt;

    @PrePersist
    private void setBlacklistedAt() {
        blacklistedAt = now();
    }
}
