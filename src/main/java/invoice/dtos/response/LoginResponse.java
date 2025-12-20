package invoice.dtos.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import invoice.data.constants.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class LoginResponse {
    @JsonFormat(pattern = "dd-MMMM-yyyy 'at' hh:mm a")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime responseTime;
    private String message;
    private String fullName;
    private String email;
    private String phone;
    private Set<Role> roles;
    private String mediaUrl;
    // Token removed - now sent as HTTP-only cookie
}
