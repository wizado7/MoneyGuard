package src.main.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequest {
    
    @NotNull(message = "Сумма не может быть пустой")
    private BigDecimal amount;
    
    private String category;
    
    @NotNull(message = "ID категории не может быть пустым")
    @Positive(message = "ID категории должен быть положительным")
    private Integer categoryId;
    
    @NotNull(message = "Дата не может быть пустой")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;
    
    @Size(max = 255, message = "Описание не может быть длиннее 255 символов")
    private String description;
    
    @Positive(message = "ID цели должен быть положительным, если указан")
    private Integer goalId;
    
    @PositiveOrZero(message = "Сумма для цели не может быть отрицательной")
    @JsonProperty("amount_to_goal")
    private BigDecimal amountToGoal;
    
    @JsonProperty("user_id")
    private String userId;
    
    public String getCategory() {
        return category;
    }
} 