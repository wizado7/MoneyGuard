package src.main.dto.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionRequest {
    
    @NotBlank(message = "Тип подписки не может быть пустым")
    @Pattern(regexp = "FREE|PREMIUM", message = "Тип подписки должен быть одним из: FREE, PREMIUM")
    private String type;
    
    private String paymentToken;
} 