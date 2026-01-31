package invoice.data.models;

import invoice.data.constants.Language;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "users_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Embedded
    @Builder.Default
    private BusinessProfileSettings profile = BusinessProfileSettings.builder().build();

    @Embedded
    @Builder.Default
    private TaxSettings taxSettings = TaxSettings.builder().build();

    @Embedded
    @Builder.Default
    private Notifications notifications = Notifications.builder().build();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Language language = Language.ENGLISH;

}
