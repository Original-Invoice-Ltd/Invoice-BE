package invoice.dtos.response;

import invoice.data.models.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private LocalDateTime createdAt;
    private List<String> roles;
    private boolean isActive;
    private String phone;
    private String imageUrl;
    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.createdAt = user.getCreatedAt();
        this.roles = Collections.singletonList(String.valueOf(user.getRoles().stream().toList()));
        this.isActive = user.isVerified();
        this.imageUrl = (user.getMediaUrl() != null) ? user.getMediaUrl() : "";    }
}
