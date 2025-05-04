package src.main.dto.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    @Builder.Default
    private boolean aiAccessEnabled = false;
    private String subscriptionType;
    private LocalDate expiryDate;
    private List<String> features;
} 