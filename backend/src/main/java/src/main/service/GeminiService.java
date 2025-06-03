package src.main.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.*;

@Service
@Slf4j
public class GeminiService {

    @Value("${google.gemini.model:gemini-2.0-flash}")
    private String modelName;

    @Value("${vpn.proxy.enabled:false}")
    private boolean vpnProxyEnabled;

    @Autowired
    private ProxyService proxyService;

    private Client geminiClient;    @PostConstruct
    public void initialize() {
        try {
            log.info("Начинаем инициализацию Gemini Service...");
            log.info("VPN Proxy Enabled: {}", vpnProxyEnabled);
            
            if (vpnProxyEnabled) {
                log.info("Настройка VPN прокси для Gemini API");
                proxyService.initialize();
                configureVpnProxy();
                log.info("VPN прокси включен для запросов к Gemini API");
                
                // Логируем текущие системные свойства прокси
                log.info("Системные свойства прокси:");
                log.info("http.proxyHost: {}", System.getProperty("http.proxyHost"));
                log.info("http.proxyPort: {}", System.getProperty("http.proxyPort"));
                log.info("https.proxyHost: {}", System.getProperty("https.proxyHost"));
                log.info("https.proxyPort: {}", System.getProperty("https.proxyPort"));
            } else {
                log.info("VPN прокси отключен");
            }
            
            // Инициализируем клиент Gemini после настройки прокси
            this.geminiClient = new Client();
            log.info("Gemini service initialized with model: {}", modelName);
            
        } catch (Exception e) {
            log.error("Ошибка инициализации Gemini сервиса: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось инициализировать Gemini сервис", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (proxyService != null) {
            proxyService.shutdown();
        }
    }    private void configureVpnProxy() {
        try {
            log.info("Начинаем конфигурацию VPN прокси...");
            
            // Устанавливаем кастомный ProxySelector для селективной маршрутизации
            ProxySelector customProxySelector = proxyService.createCustomProxySelector();
            ProxySelector.setDefault(customProxySelector);
            log.info("Установлен кастомный ProxySelector");
            
            // Также устанавливаем системные свойства для HTTP прокси
            String currentProxyUrl = proxyService.getCurrentProxyUrl();
            log.info("Текущий прокси URL: {}", currentProxyUrl);
            
            if (currentProxyUrl != null && !currentProxyUrl.isEmpty() && !currentProxyUrl.contains("Direct connection")) {
                try {
                    // Парсим прокси URL (формат может быть /host:port)
                    String host;
                    int port;
                    
                    if (currentProxyUrl.startsWith("/")) {
                        // Формат: /host:port
                        String[] parts = currentProxyUrl.substring(1).split(":");
                        host = parts[0];
                        port = Integer.parseInt(parts[1]);
                    } else if (currentProxyUrl.startsWith("http://")) {
                        // Формат: http://host:port
                        URL proxyURL = new URL(currentProxyUrl);
                        host = proxyURL.getHost();
                        port = proxyURL.getPort();
                    } else {
                        log.warn("Неизвестный формат прокси URL: {}", currentProxyUrl);
                        return;
                    }
                    
                    // Устанавливаем системные свойства для HTTP и HTTPS прокси
                    System.setProperty("http.proxyHost", host);
                    System.setProperty("http.proxyPort", String.valueOf(port));
                    System.setProperty("https.proxyHost", host);
                    System.setProperty("https.proxyPort", String.valueOf(port));
                    
                    log.info("Системные свойства прокси установлены: {}:{}", host, port);
                } catch (Exception e) {
                    log.warn("Не удалось разобрать URL прокси: {}, ошибка: {}", currentProxyUrl, e.getMessage());
                }
            } else {
                log.warn("Прокси URL пуст или указывает на прямое соединение: {}", currentProxyUrl);
            }
            
            log.info("VPN прокси успешно настроен для Gemini API запросов");
            
            // Логируем статус прокси
            proxyService.getProxyStatus().forEach(log::info);
            
        } catch (Exception e) {
            log.error("Ошибка настройки VPN прокси: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось настроить VPN прокси", e);
        }
    }

    public String generateResponse(String userMessage) {
        try {
            // Проверяем статус прокси перед отправкой запроса
            if (vpnProxyEnabled) {
                ensureProxyIsWorking();
            }
            
            log.info("Отправка запроса к Gemini API с сообщением длиной: {} символов", userMessage.length());
            log.debug("Текст запроса: {}", userMessage);
            
            GenerateContentResponse response = geminiClient.models.generateContent(
                modelName,
                userMessage,
                null
            );
            
            String result = response.text();
            log.info("Получен ответ от Gemini API, длина: {} символов", result != null ? result.length() : 0);
            log.debug("Текст ответа: {}", result);
            
            return result != null ? result : "Не удалось получить ответ от AI.";
            
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
                        configureVpnProxy(); // Переконфигурируем с новым прокси
                    }
                } catch (Exception proxyError) {
                    log.error("Ошибка при попытке переключения прокси: {}", proxyError.getMessage());
                }
            }
            
            return "Извините, произошла ошибка при обработке вашего запроса. Попробуйте позже.";
        }
    }    private void ensureProxyIsWorking() {
        try {
            String currentProxy = proxyService.getCurrentProxyUrl();
            if (currentProxy == null || !proxyService.isProxyHealthy(currentProxy)) {
                log.warn("Текущий прокси не работает, ищем рабочий прокси...");
                proxyService.checkAllProxiesHealth();
                
                // Переконфигурируем системные свойства прокси
                String newProxy = proxyService.getCurrentProxyUrl();
                if (newProxy != null && !newProxy.equals(currentProxy)) {
                    configureVpnProxy();
                    log.info("Переключились на новый прокси: {}", newProxy);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке статуса прокси: {}", e.getMessage());
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
}
