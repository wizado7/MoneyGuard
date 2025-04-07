package src.main.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private Long id;
    private String email;
    private String name;
    private LocalDateTime created_at;
    private boolean ai_access_enabled;
    private String subscriptionType;
    private LocalDate subscriptionExpiry;
} 