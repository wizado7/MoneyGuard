package src.main.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import src.main.dto.ai.AIChatRequest;
import src.main.dto.ai.AIChatResponse;
import src.main.dto.ai.AnalysisResponse;
import src.main.exception.BusinessException;
import src.main.exception.InvalidDataException;
import src.main.exception.ResourceNotFoundException;
import src.main.model.ChatHistory;
import src.main.model.SubscriptionType;
import src.main.model.User;
import src.main.repository.ChatHistoryRepository;
import src.main.repository.UserRepository;
import src.main.service.AIService;

import java.util.*;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AIController {

    private final AIService aiService;
    private final UserRepository userRepository;
    private final ChatHistoryRepository chatHistoryRepository;

    @GetMapping("/analysis")
    public ResponseEntity<AnalysisResponse> getAnalysis() {
        log.debug("REST запрос на получение аналитики");
        try {
            return ResponseEntity.ok(aiService.getAnalysis());
        } catch (Exception e) {
            log.error("Ошибка при получении аналитики: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при получении аналитики", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AIChatResponse> chat(
            @RequestParam @NotBlank(message = "Сообщение не может быть пустым") String message,
            @RequestParam(required = false) MultipartFile image) {
        log.debug("REST запрос на чат с ИИ: {}", message);
        
        // Проверяем доступ к AI чату
        User currentUser = getCurrentUser();
        if (!currentUser.isAiAccessEnabled()) {
            throw new AccessDeniedException("AI доступ отключен для данного пользователя");
        }
        
        try {
            if (image != null && !image.isEmpty()) {
                String fileName = image.getOriginalFilename();
                if (fileName != null && !(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png"))) {
                    throw new InvalidDataException("Неподдерживаемый формат изображения")
                            .addError("image", "Поддерживаются только изображения в форматах JPG, JPEG и PNG");
                }
                
                if (image.getSize() > 5 * 1024 * 1024) { // 5 MB
                    throw new InvalidDataException("Слишком большой размер изображения")
                            .addError("image", "Размер изображения не должен превышать 5 МБ");
                }
            }
            
            return ResponseEntity.ok(aiService.chat(message, image));
        } catch (InvalidDataException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса к ИИ: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при обработке запроса к ИИ", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<AIChatResponse> getAIRecommendations(@RequestBody AIChatRequest request) {
        log.debug("REST запрос на AI рекомендации: {}", request.getMessage());
        
        // AI чат доступен всем пользователям с включенным AI доступом
        User currentUser = getCurrentUser();
        if (!currentUser.isAiAccessEnabled()) {
            throw new AccessDeniedException("AI доступ отключен для данного пользователя");
        }
        
        try {
            return ResponseEntity.ok(aiService.chat(request.getMessage(), null));
        } catch (Exception e) {
            log.error("Ошибка при получении AI рекомендаций: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при получении AI рекомендаций", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/chat/history")
    public ResponseEntity<List<Map<String, Object>>> getChatHistory() {
        log.debug("REST запрос на получение истории чата");
        
        User currentUser = getCurrentUser();
        
        // История чата доступна всем пользователям с включенным AI доступом
        if (!currentUser.isAiAccessEnabled()) {
            throw new AccessDeniedException("AI доступ отключен для данного пользователя");
        }
        
        try {
            List<ChatHistory> history = chatHistoryRepository.findByUserOrderByCreatedAtAsc(currentUser);
            
            List<Map<String, Object>> messages = new ArrayList<>();
            for (ChatHistory entry : history) {
                Map<String, Object> message = new HashMap<>();
                message.put("role", entry.getRole().toString());
                message.put("message", entry.getMessage() != null ? entry.getMessage() : "");
                message.put("response", entry.getResponse() != null ? entry.getResponse() : "");
                message.put("createdAt", entry.getCreatedAt().toString());
                messages.add(message);
            }
            
            
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Ошибка при получении истории чата: {}", e.getMessage(), e);
            throw new BusinessException("Ошибка при получении истории чата", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
 