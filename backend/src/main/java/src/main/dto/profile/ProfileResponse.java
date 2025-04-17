package src.main.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import src.main.model.SubscriptionType;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private Integer id;
    private String email;
    private String name;
    private String profileImage;
    private boolean aiAccessEnabled;
    private String subscriptionType;
    private LocalDate subscriptionExpiry;
} 