package src.main.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import src.main.model.ChatHistory;

@Service
@Slf4j
public class HttpGeminiService {

    @Value("${google.gemini.model:gemini-2.0-flash}")
    private String modelName;

    @Value("${google.gemini.api-key:${GOOGLE_GEMINI_API_KEY:}}")
    private String apiKey;    @Value("${vpn.proxy.enabled:false}")
    private boolean vpnProxyEnabled;

    private HttpClient httpClient;
    private ObjectMapper objectMapper;@PostConstruct
    public void initialize() {
        try {
            log.info("Инициализация HTTP Gemini Service...");
            log.info("VPN Proxy включен: {}", vpnProxyEnabled);
            log.info("API Key настроен: {}", apiKey != null && !apiKey.trim().isEmpty());
            log.info("Модель: {}", modelName);

            objectMapper = new ObjectMapper();

            // Создаем HttpClient - либо с прокси, либо без
            if (vpnProxyEnabled) {
                httpClient = createProxyHttpClient();
                log.info("HTTP клиент создан с прокси конфигурацией");
            } else {
                httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(60))
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();
                log.info("HTTP клиент создан БЕЗ прокси");
            }

            log.info("HTTP Gemini Service успешно инициализирован");

        } catch (Exception e) {
            log.error("Ошибка при инициализации HTTP Gemini Service: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось инициализировать HTTP Gemini Service", e);
        }
    }

    /**
     * Создает HttpClient с прокси точно как в curl: -x "http://ZTU6FR:Xkv2tk@45.155.201.91:8000"
     */
    private HttpClient createProxyHttpClient() {
        log.info("🔧 Создаем HttpClient с прокси как в curl...");
        
        // Настраиваем прокси
        InetSocketAddress proxyAddress = new InetSocketAddress("45.155.201.91", 8000);
        ProxySelector proxySelector = ProxySelector.of(proxyAddress);
        
        // Настраиваем аутентификацию для прокси
        Authenticator proxyAuthenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                RequestorType requestorType = getRequestorType();
                String requestingHost = getRequestingHost();
                int requestingPort = getRequestingPort();
                
                log.debug("Аутентификация: Тип={}, Хост={}, Порт={}", 
                         requestorType, requestingHost, requestingPort);
                
                // Возвращаем данные только для нашего прокси
                if (requestorType == RequestorType.PROXY && 
                    "45.155.201.91".equals(requestingHost) && requestingPort == 8000) {
                    
                    log.info("Предоставляем аутентификацию для прокси: ZTU6FR");
                    return new PasswordAuthentication("ZTU6FR", "Xkv2tk".toCharArray());
                }
                
                return null;
            }
        };

        // Важно: используем HTTP/1.1 для совместимости с CONNECT tunneling
        HttpClient client = HttpClient.newBuilder()
                .proxy(proxySelector)
                .authenticator(proxyAuthenticator)
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(60))
                .build();

        log.info("HttpClient с прокси готов: 45.155.201.91:8000 (ZTU6FR:***)");
        return client;    }@PreDestroy
    public void cleanup() {
        log.info("Завершение работы HTTP Gemini Service");
    }    public String generateResponse(String userMessage) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.error("Google Gemini API key не настроен");
                throw new RuntimeException("Google Gemini API key не настроен");
            }

            log.info("=== ОТПРАВКА ЗАПРОСА К GEMINI API ===");
            log.info("VPN Proxy включен: {}", vpnProxyEnabled);
            log.info("Модель: {}, Сообщение: {} символов", modelName, userMessage.length());

            // Используем curl если прокси включен, иначе стандартный HttpClient
            if (vpnProxyEnabled) {
                return generateResponseViaCurl(userMessage);
            } else {
                return generateResponseViaHttpClient(userMessage);
            }

        } catch (Exception e) {
            log.error("Ошибка при обращении к Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при обращении к Google Gemini API: " + e.getMessage(), e);
        }
    }

    /**
     * Отправляет запрос через curl (используется когда включен прокси)
     */
    private String generateResponseViaCurl(String userMessage) {
        try {
            log.info("Отправляем запрос через curl с прокси...");

            // Создаем JSON запрос
            GeminiRequest request = new GeminiRequest();
            request.setContents(List.of(new ContentPart(List.of(new TextPart(userMessage)))));
            String jsonBody = objectMapper.writeValueAsString(request);

            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    modelName, apiKey);

            // Команда curl точно как в рабочем примере
            ProcessBuilder pb = new ProcessBuilder(
                    "curl", "-s", "-X", "POST",
                    "-H", "Content-Type: application/json",
                    "-d", jsonBody,
                    "--proxy", "http://ZTU6FR:Xkv2tk@45.155.201.91:8000",
                    "--connect-timeout", "60",
                    "--max-time", "120",
                    url
            );

            pb.redirectErrorStream(true);
            log.info("🔧 Выполняем curl команду через прокси");

            Process process = pb.start();

            // Читаем ответ
            String result = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            log.info("Curl завершен с кодом: {}, Длина ответа: {} символов", exitCode, result.length());

            if (exitCode != 0) {
                log.error("Curl завершился с ошибкой. Код: {}, Вывод: {}", exitCode, result);
                throw new RuntimeException("Curl failed with exit code: " + exitCode + ", Output: " + result);
            }

            if (result == null || result.trim().isEmpty()) {
                log.error("Пустой ответ от curl");
                throw new RuntimeException("Пустой ответ от curl");
            }

            log.debug("Ответ curl (первые 200 символов): {}",
                    result.length() > 200 ? result.substring(0, 200) + "..." : result);

            return parseGeminiResponse(result);

        } catch (Exception e) {
            log.error("Ошибка curl запроса: {}", e.getMessage(), e);
            throw new RuntimeException("Curl request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Отправляет запрос через стандартный HttpClient (без прокси)
     */
    private String generateResponseViaHttpClient(String userMessage) {
        try {
            log.info("Отправляем запрос через HttpClient без прокси...");

            // Создаем JSON запрос
            GeminiRequest request = new GeminiRequest();
            request.setContents(List.of(new ContentPart(List.of(new TextPart(userMessage)))));
            String jsonBody = objectMapper.writeValueAsString(request);

            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    modelName, apiKey);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofMinutes(2))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            log.info("✅ Получен ответ от Gemini API. Статус: {}", response.statusCode());

            if (response.statusCode() != 200) {
                String errorBody = response.body() != null ? response.body() : "No body";
                log.error("Ошибка API: HTTP {}, Тело: {}", response.statusCode(), errorBody);
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + errorBody);
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new RuntimeException("Пустой ответ от Gemini API");
            }

            return parseGeminiResponse(responseBody);

        } catch (Exception e) {
            log.error("Ошибка HttpClient запроса: {}", e.getMessage(), e);
            throw new RuntimeException("HttpClient request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Парсит ответ от Gemini API
     */
    private String parseGeminiResponse(String responseBody) {
        try {
            GeminiResponse geminiResponse = objectMapper.readValue(responseBody, GeminiResponse.class);

            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                Candidate candidate = geminiResponse.getCandidates().get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null &&
                        !candidate.getContent().getParts().isEmpty()) {

                    String result = candidate.getContent().getParts().get(0).getText();
                    log.info("Успешно получен ответ от Gemini, длина: {} символов", result != null ? result.length() : 0);
                    return result != null ? result : "Не удалось получить ответ от AI.";
                }
            }

            log.warn("Получен пустой или некорректный ответ от Gemini API");
            return "Не удалось получить ответ от AI.";

        } catch (Exception e) {
            log.error("Ошибка парсинга ответа Gemini: {}", e.getMessage());
            log.debug("Тело ответа: {}", responseBody);
            throw new RuntimeException("Ошибка парсинга ответа: " + e.getMessage());
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
    }    public String getProxyStatus() {
        if (!vpnProxyEnabled) {
            return "VPN прокси отключен";
        }
        
        return "VPN прокси включен: 45.155.201.91:8000 (ZTU6FR:***)";
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
