package src.main.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    
    @NotBlank(message = "Название категории не может быть пустым")
    @Size(min = 2, max = 50, message = "Название категории должно содержать от 2 до 50 символов")
    private String name;
    
    private Integer parent_id;
    
    @Size(max = 50, message = "Иконка должна содержать не более 50 символов")
    @Pattern(regexp = "^[a-z\\-]*$", message = "Иконка должна содержать только строчные буквы и дефисы")
    private String icon;

    private Boolean isIncome;

    @Size(max = 7, message = "Цвет должен быть в формате HEX (#RRGGBB)")
    private String color;
} 