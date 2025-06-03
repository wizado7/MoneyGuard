package src.main.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class HttpGeminiService {

    @Value("${google.gemini.model:gemini-2.0-flash}")
    private String modelName;

    @Value("${google.gemini.api-key:${GOOGLE_GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${vpn.proxy.enabled:false}")
    private boolean vpnProxyEnabled;

    @Autowired
    private ProxyService proxyService;

    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void initialize() {
        try {
            log.info("Начинаем инициализацию HTTP Gemini Service...");
            log.info("VPN Proxy Enabled: {}", vpnProxyEnabled);
            log.info("API Key present: {}", apiKey != null && !apiKey.trim().isEmpty());
            log.info("Model Name: {}", modelName);

            objectMapper = new ObjectMapper();

            // Создаем HttpClient с принудительным использованием прокси
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .version(HttpClient.Version.HTTP_1_1);

            if (vpnProxyEnabled && proxyService != null) {
                configureProxy(builder);
            }

            httpClient = builder.build();
            log.info("HTTP Gemini Service успешно инициализирован");

        } catch (Exception e) {
            log.error("Ошибка при инициализации HTTP Gemini Service: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось инициализировать HTTP Gemini Service", e);
        }
    }

    private void configureProxy(HttpClient.Builder builder) {
        try {
            String currentProxyUrl = proxyService.getCurrentProxyUrl();
            if (currentProxyUrl != null) {
                log.info("Настраиваем прокси для HTTP клиента: {}", currentProxyUrl);
                
                URI proxyUri = URI.create(currentProxyUrl);
                InetSocketAddress proxyAddress = new InetSocketAddress(
                        proxyUri.getHost(), 
                        proxyUri.getPort()
                );
                
                ProxySelector proxySelector = ProxySelector.of(proxyAddress);
                builder.proxy(proxySelector);
                
                log.info("Прокси успешно настроен: {}:{}", proxyUri.getHost(), proxyUri.getPort());
            } else {
                log.warn("Текущий прокси URL не найден, работаем без прокси");
            }
        } catch (Exception e) {
            log.error("Ошибка при настройке прокси: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("Завершение работы HTTP Gemini Service");
    }

    public String generateResponse(String userMessage) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.error("Google Gemini API key не настроен");
                throw new RuntimeException("Google Gemini API key не настроен");
            }

            // Проверяем статус прокси перед отправкой запроса
            if (vpnProxyEnabled) {
                ensureProxyIsWorking();
            }

            log.info("Отправка запроса к Gemini API с сообщением длиной: {} символов", userMessage.length());
            log.debug("Текст запроса: {}", userMessage);

            // Создаем запрос к Google Gemini API
            GeminiRequest request = new GeminiRequest();
            request.setContents(List.of(new ContentPart(List.of(new TextPart(userMessage)))));

            String jsonBody = objectMapper.writeValueAsString(request);
            log.debug("JSON запрос: {}", jsonBody);

            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", 
                    modelName, apiKey);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            log.info("Получен ответ от Gemini API. Status: {}, Body length: {}", 
                    response.statusCode(), response.body().length());
            log.debug("Response body: {}", response.body());

            if (response.statusCode() != 200) {
                log.error("Ошибка API: HTTP {}, Body: {}", response.statusCode(), response.body());
                throw new RuntimeException("Google Gemini API вернул ошибку: HTTP " + response.statusCode());
            }

            // Парсим ответ
            GeminiResponse geminiResponse = objectMapper.readValue(response.body(), GeminiResponse.class);
            
            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                Candidate candidate = geminiResponse.getCandidates().get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null && 
                    !candidate.getContent().getParts().isEmpty()) {
                    
                    String result = candidate.getContent().getParts().get(0).getText();
                    log.info("Получен ответ от Gemini API, длина: {} символов", result != null ? result.length() : 0);
                    return result != null ? result : "Не удалось получить ответ от AI.";
                }
            }

            log.warn("Получен пустой или некорректный ответ от Gemini API");
            return "Не удалось получить ответ от AI.";

        } catch (Exception e) {
            log.error("Ошибка при обращении к Gemini API: {}", e.getMessage(), e);
            
            // Логируем информацию о прокси при ошибке
            if (vpnProxyEnabled && proxyService != null) {
                log.info("Статус прокси при ошибке:");
                proxyService.getProxyStatus().forEach(log::warn);
                
                // Пытаемся переключиться на другой прокси если текущий не работает
                try {
                    String currentProxy = proxyService.getCurrentProxyUrl();
                    if (currentProxy != null && !proxyService.isProxyHealthy(currentProxy)) {
                        log.info("Текущий прокси не работает, пытаемся найти рабочий...");
                        proxyService.checkAllProxiesHealth();
                        
                        // Пересоздаем HTTP клиент с новым прокси
                        initialize();
                    }
                } catch (Exception proxyError) {
                    log.error("Ошибка при переключении прокси: {}", proxyError.getMessage());
                }
            }
            
            throw new RuntimeException("Ошибка при обращении к Google Gemini API: " + e.getMessage(), e);
        }
    }

    private void ensureProxyIsWorking() {
        if (proxyService == null) {
            log.warn("ProxyService не инициализирован");
            return;
        }

        try {
            String currentProxy = proxyService.getCurrentProxyUrl();
            if (currentProxy == null) {
                log.warn("Текущий прокси не установлен");
                return;
            }

            if (!proxyService.isProxyHealthy(currentProxy)) {
                log.warn("Текущий прокси {} не работает, ищем рабочий...", currentProxy);
                proxyService.checkAllProxiesHealth();
                
                // Пересоздаем HTTP клиент с новым прокси
                initialize();
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке прокси: {}", e.getMessage(), e);
        }
    }

    public String generateFinancialAdvice(String context) {
        String prompt = String.format(
            "Ты - эксперт по личным финансам. Проанализируй следующую финансовую информацию и дай краткие, практичные советы:\n\n%s\n\n" +
            "Дай 3-4 конкретных совета по улучшению финансового положения. Отвечай на русском языке, кратко и по делу.",
            context
        );
        
        return generateResponse(prompt);
    }

    public String analyzeTransaction(String transactionInfo) {
        String prompt = String.format(
            "Проанализируй эту финансовую транзакцию и дай краткий совет:\n\n%s\n\n" +
            "Дай 1-2 практических совета или замечания. Отвечай на русском языке, кратко.",
            transactionInfo
        );
        
        return generateResponse(prompt);
    }

    public String getProxyStatus() {
        if (!vpnProxyEnabled || proxyService == null) {
            return "VPN прокси отключен";
        }
        
        return String.join("\n", proxyService.getProxyStatus());
    }

    // DTO классы для работы с Gemini API
    @Data
    public static class GeminiRequest {
        private List<ContentPart> contents;
    }

    @Data
    public static class ContentPart {
        private List<TextPart> parts;

        public ContentPart(List<TextPart> parts) {
            this.parts = parts;
        }
    }

    @Data
    public static class TextPart {
        private String text;

        public TextPart(String text) {
            this.text = text;
        }
    }    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiResponse {
        private List<Candidate> candidates;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        private ContentResponse content;
    }    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentResponse {
        private List<PartResponse> parts;
        private String role;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PartResponse {
        private String text;
    }
}
