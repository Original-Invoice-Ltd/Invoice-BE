package invoice.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OTPResponse {
    private boolean success;
    private String message;
    private int remainingAttempts;
    private boolean isExpired;
    private LocalDateTime responseTime;
}