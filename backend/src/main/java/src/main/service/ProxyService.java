package src.main.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ProxyService {

    @Value("${vpn.proxy.urls:}")
    private String proxyUrls;

    @Value("${vpn.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${vpn.proxy.rotation.enabled:true}")
    private boolean rotationEnabled;

    @Value("${vpn.proxy.rotation.interval:300000}")
    private long rotationInterval;

    @Value("${vpn.proxy.connection.timeout:10000}")
    private int connectionTimeout;

    @Value("${vpn.proxy.read.timeout:30000}")
    private int readTimeout;

    @Value("${vpn.proxy.fallback.direct:true}")
    private boolean fallbackDirect;

    @Value("${vpn.proxy.auth.url:}")
    private String authProxyUrl;

    @Value("${vpn.proxy.user:}")
    private String proxyUser;

    @Value("${vpn.proxy.password:}")
    private String proxyPassword;

    private final List<Proxy> proxyList = new ArrayList<>();
    private final Map<Proxy, ProxyHealth> proxyHealth = new ConcurrentHashMap<>();
    private final AtomicInteger currentProxyIndex = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);    private volatile boolean initialized = false;    @PostConstruct
    public void initialize() {
        log.info("=== Инициализация ProxyService ===");
        log.info("proxyEnabled: {}", proxyEnabled);
        log.info("proxyUrls: {}", proxyUrls);
        log.info("authProxyUrl: {}", authProxyUrl);
        log.info("proxyUser: {}", proxyUser != null ? "***" : "null");
        log.info("proxyPassword: {}", proxyPassword != null ? "***" : "null");
        log.info("rotationEnabled: {}", rotationEnabled);
        
        if (!proxyEnabled || initialized) {
            log.warn("ProxyService не инициализирован: proxyEnabled={}, initialized={}", proxyEnabled, initialized);
            return;
        }

        try {
            // СНАЧАЛА настраиваем аутентификацию
            setupAuthenticator();
            
            parseProxyUrls();
            if (rotationEnabled) {
                startHealthCheck();
                startProxyRotation();
            }
            initialized = true;
            log.info("ProxyService успешно инициализирован с {} прокси", proxyList.size());
            
            // Выводим список всех прокси для отладки
            for (int i = 0; i < proxyList.size(); i++) {
                Proxy proxy = proxyList.get(i);
                log.info("Прокси {}: {}", i + 1, proxy.address());
            }
        } catch (Exception e) {
            log.error("Ошибка инициализации ProxyService: {}", e.getMessage(), e);
        }
    }    private void parseProxyUrls() {
        log.info("Начинаем парсинг прокси URLs: '{}'", proxyUrls);
        
        if (proxyUrls == null || proxyUrls.trim().isEmpty()) {
            log.warn("Список прокси URL пуст или null");
            return;
        }

        String[] urls = proxyUrls.split(",");
        log.info("Найдено {} потенциальных прокси URL", urls.length);
        
        for (String url : urls) {
            try {
                String trimmedUrl = url.trim();
                log.debug("Обрабатываем URL: '{}'", trimmedUrl);
                
                URI uri = URI.create(trimmedUrl);
                String host = uri.getHost();
                int port = uri.getPort();
                
                if (host != null && port > 0) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
                    proxyList.add(proxy);
                    proxyHealth.put(proxy, new ProxyHealth());
                    log.info("Добавлен прокси: {}:{}", host, port);
                } else {
                    log.warn("Некорректный прокси URL (нет хоста или порта): {}", trimmedUrl);
                }
            } catch (Exception e) {
                log.warn("Некорректный прокси URL: {}, ошибка: {}", url, e.getMessage());
            }
        }

        // Add auth proxy if configured
        if (authProxyUrl != null && !authProxyUrl.trim().isEmpty()) {
            try {
                URI uri = URI.create(authProxyUrl.trim());
                String host = uri.getHost();
                int port = uri.getPort();
                
                if (host != null && port > 0) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
                    proxyList.add(proxy);
                    proxyHealth.put(proxy, new ProxyHealth());
                    log.info("Added auth proxy: {}:{}", host, port);
                }
            } catch (Exception e) {
                log.warn("Invalid auth proxy URL: {}, error: {}", authProxyUrl, e.getMessage());
            }
        }
    }    private void setupAuthenticator() {
        if (proxyUser != null && !proxyUser.isEmpty() && 
            proxyPassword != null && !proxyPassword.isEmpty()) {
            
            log.info("Данные аутентификации прокси готовы для пользователя: {}", proxyUser);
            log.info("Аутентификация будет выполняться через HttpClient Authenticator");
        } else {
            log.warn("Данные аутентификации прокси отсутствуют - user: {}, password: {}", 
                    proxyUser != null, proxyPassword != null);
        }
    }private void startHealthCheck() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                checkProxyHealth();
                // Периодически проверяем скорость прокси
                performSpeedTests();
            } catch (Exception e) {
                log.error("Error during proxy health check: {}", e.getMessage());
            }
        }, 30, 60, TimeUnit.SECONDS);
    }

    private void performSpeedTests() {
        List<Proxy> proxiesNeedingSpeedTest = proxyList.stream()
                .filter(proxy -> {
                    ProxyHealth health = proxyHealth.get(proxy);
                    return health != null && health.isHealthy() && health.needsSpeedTest();
                })
                .limit(3) // Проверяем максимум 3 прокси за раз, чтобы не перегружать
                .toList();

        if (!proxiesNeedingSpeedTest.isEmpty()) {
            log.info("🏃 Performing speed tests for {} proxies", proxiesNeedingSpeedTest.size());
            for (Proxy proxy : proxiesNeedingSpeedTest) {
                testProxyConnection(proxy);
            }
        }
    }

    private void startProxyRotation() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                rotateProxy();
            } catch (Exception e) {
                log.error("Error during proxy rotation: {}", e.getMessage());
            }
        }, rotationInterval, rotationInterval, TimeUnit.MILLISECONDS);
    }

    private void checkProxyHealth() {
        for (Proxy proxy : proxyList) {
            ProxyHealth health = proxyHealth.get(proxy);
            if (health != null) {
                boolean isHealthy = testProxyConnection(proxy);
                health.updateHealth(isHealthy);
                
                if (isHealthy) {
                    log.debug("Proxy {} is healthy", proxy.address());
                } else {
                    log.warn("Proxy {} is unhealthy", proxy.address());
                }
            }
        }
    }    private boolean testProxyConnection(Proxy proxy) {
        try {
            long startTime = System.currentTimeMillis();
            
            // Use a simple HTTP endpoint for testing to avoid SSL issues
            URL testUrl = new URL("http://httpbin.org/ip");
            HttpURLConnection connection = (HttpURLConnection) testUrl.openConnection(proxy);
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;
            connection.disconnect();
            
            boolean isHealthy = responseCode >= 200 && responseCode < 400;
            
            // Обновляем здоровье с учётом времени отклика
            ProxyHealth health = proxyHealth.get(proxy);
            if (health != null) {
                health.updateHealth(isHealthy, responseTime);
                health.markSpeedTested();
            }
            
            if (isHealthy) {
                log.info("Proxy {} health check successful (response: {}, time: {}ms)", 
                        proxy.address(), responseCode, responseTime);
            } else {
                log.warn("Proxy {} health check failed with response code: {}", proxy.address(), responseCode);
            }
            return isHealthy;
        } catch (Exception e) {
            log.warn("Proxy {} health check failed: {}", proxy.address(), e.getMessage());
            
            // Помечаем как нездоровый с максимальным временем отклика
            ProxyHealth health = proxyHealth.get(proxy);
            if (health != null) {
                health.updateHealth(false, Long.MAX_VALUE);
            }
            return false;
        }
    }

    private void rotateProxy() {
        if (proxyList.isEmpty()) {
            return;
        }

        List<Proxy> healthyProxies = getHealthyProxies();
        if (!healthyProxies.isEmpty()) {
            int nextIndex = (currentProxyIndex.get() + 1) % healthyProxies.size();
            currentProxyIndex.set(nextIndex);
            log.debug("Rotated to proxy index: {}", nextIndex);
        }
    }    public Proxy getCurrentProxy() {
        if (!proxyEnabled || !initialized) {
            return Proxy.NO_PROXY;
        }

        // Приоритет платному аутентифицированному прокси
        if (authProxyUrl != null && !authProxyUrl.trim().isEmpty()) {
            for (Proxy proxy : proxyList) {
                if (proxy.address().toString().contains("45.155.201.91")) {
                    ProxyHealth health = proxyHealth.get(proxy);
                    if (health != null && health.isHealthy()) {
                        log.debug("Используем платный США прокси: {}", proxy.address());
                        return proxy;
                    }
                }
            }
        }

        List<Proxy> healthyProxies = getHealthyProxies();
        if (healthyProxies.isEmpty()) {
            if (fallbackDirect) {
                log.warn("No healthy proxies available, falling back to direct connection");
                return Proxy.NO_PROXY;
            } else {
                // Try to use any proxy even if unhealthy
                if (!proxyList.isEmpty()) {
                    return proxyList.get(currentProxyIndex.get() % proxyList.size());
                }
                return Proxy.NO_PROXY;
            }
        }

        // Получаем самый быстрый прокси
        Proxy fastestProxy = getFastestProxy(healthyProxies);
        if (fastestProxy != null) {
            return fastestProxy;
        }

        // Fallback на ротацию если не можем определить самый быстрый
        int index = currentProxyIndex.get() % healthyProxies.size();
        return healthyProxies.get(index);
    }

    private Proxy getFastestProxy(List<Proxy> healthyProxies) {
        return healthyProxies.stream()
                .min((p1, p2) -> {
                    ProxyHealth health1 = proxyHealth.get(p1);
                    ProxyHealth health2 = proxyHealth.get(p2);
                    
                    if (health1 == null || health2 == null) {
                        return 0;
                    }
                    
                    return Long.compare(health1.getResponseTime(), health2.getResponseTime());
                })
                .orElse(null);
    }

    private List<Proxy> getHealthyProxies() {
        return proxyList.stream()
                .filter(proxy -> {
                    ProxyHealth health = proxyHealth.get(proxy);
                    return health != null && health.isHealthy();
                })
                .toList();
    }

    public ProxySelector createCustomProxySelector() {
        return new ProxySelector() {
            private final ProxySelector defaultSelector = ProxySelector.getDefault();            @Override
            public List<Proxy> select(URI uri) {
                log.debug("ProxySelector.select() вызван для URI: {}", uri);
                
                // Check if this is a request to Google APIs
                if (uri.getHost() != null && 
                    (uri.getHost().contains("googleapis.com") || 
                     uri.getHost().contains("generativelanguage.googleapis.com") ||
                     uri.getHost().contains("google.com"))) {
                    
                    Proxy currentProxy = getCurrentProxy();
                    if (currentProxy != Proxy.NO_PROXY) {
                        log.info("Направляем запрос к {} через прокси: {}", uri.getHost(), currentProxy.address());
                        return List.of(currentProxy);
                    } else {
                        log.warn("Прокси недоступен для запроса к {}, использую прямое подключение", uri.getHost());
                    }
                }
                
                // For all other requests, use default selector
                List<Proxy> result = defaultSelector != null ? defaultSelector.select(uri) : List.of(Proxy.NO_PROXY);
                log.debug("Для URI {} используется стандартный селектор: {} прокси", uri.getHost(), result.size());
                return result;
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                log.error("Connection failed to {} via {}: {}", uri, sa, ioe.getMessage());
                
                // Mark proxy as unhealthy if connection failed
                for (Proxy proxy : proxyList) {
                    if (proxy.address().equals(sa)) {
                        ProxyHealth health = proxyHealth.get(proxy);
                        if (health != null) {
                            health.markUnhealthy();
                        }
                        break;
                    }
                }
                
                if (defaultSelector != null) {
                    defaultSelector.connectFailed(uri, sa, ioe);
                }
            }
        };
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }    public List<String> getProxyStatus() {
        List<String> status = new ArrayList<>();
        status.add(String.format("Proxy Service: %s", proxyEnabled ? "Enabled" : "Disabled"));
        status.add(String.format("Total Proxies: %d", proxyList.size()));
        status.add(String.format("Healthy Proxies: %d", getHealthyProxies().size()));
        
        // Получаем текущий (самый быстрый) прокси
        Proxy currentProxy = getCurrentProxy();
        
        for (int i = 0; i < proxyList.size(); i++) {
            Proxy proxy = proxyList.get(i);
            ProxyHealth health = proxyHealth.get(proxy);
            String healthStatus = health != null ? (health.isHealthy() ? "Healthy" : "Unhealthy") : "Unknown";
            long responseTime = health != null ? health.getResponseTime() : Long.MAX_VALUE;
            String timeStr = responseTime == Long.MAX_VALUE ? "N/A" : responseTime + "ms";
            boolean isCurrent = proxy.equals(currentProxy);
            boolean isFastest = isCurrent && health != null && health.getResponseTime() != Long.MAX_VALUE;
            
            status.add(String.format("Proxy %d: %s [%s] [%s] %s %s", 
                i + 1, proxy.address(), healthStatus, timeStr, 
                isCurrent ? "(Current)" : "",
                isFastest ? "" : ""));
        }
        
        return status;
    }

    public Map<String, Boolean> getAllProxyStatuses() {
        Map<String, Boolean> statuses = new HashMap<>();
        for (Proxy proxy : proxyList) {
            String proxyUrl = proxy.address().toString();
            ProxyHealth health = proxyHealth.get(proxy);
            boolean isHealthy = health != null && health.isHealthy();
            statuses.put(proxyUrl, isHealthy);
        }
        return statuses;
    }

    public String getCurrentProxyUrl() {
        Proxy currentProxy = getCurrentProxy();
        if (currentProxy == Proxy.NO_PROXY) {
            return "Direct connection (no proxy)";
        }
        return currentProxy.address().toString();
    }

    public boolean isProxyHealthy(String proxyUrl) {
        for (Proxy proxy : proxyList) {
            if (proxy.address().toString().equals(proxyUrl)) {
                ProxyHealth health = proxyHealth.get(proxy);
                return health != null && health.isHealthy();
            }
        }
        return false;
    }    public void checkAllProxiesHealth() {
        log.info("Manually triggering health check for all proxies");
        checkProxyHealth();
    }

    /**
     * Получение данных аутентификации для прокси
     */
    public PasswordAuthentication getProxyAuthentication() {
        if (proxyUser != null && !proxyUser.isEmpty() && 
            proxyPassword != null && !proxyPassword.isEmpty()) {
            return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
        }
        return null;
    }

    /**
     * Проверка, требуется ли аутентификация для прокси
     */
    public boolean requiresAuthentication() {
        return proxyUser != null && !proxyUser.isEmpty() && 
               proxyPassword != null && !proxyPassword.isEmpty();
    }private static class ProxyHealth {
        private volatile boolean healthy = true;
        private volatile long lastCheck = System.currentTimeMillis();
        private volatile int consecutiveFailures = 0;
        private volatile long responseTime = Long.MAX_VALUE; // Время отклика в миллисекундах
        private volatile long lastSpeedTest = 0;

        public boolean isHealthy() {
            return healthy && consecutiveFailures < 3;
        }

        public void updateHealth(boolean isHealthy, long responseTime) {
            this.lastCheck = System.currentTimeMillis();
            this.responseTime = responseTime;
            if (isHealthy) {
                this.healthy = true;
                this.consecutiveFailures = 0;
            } else {
                this.consecutiveFailures++;
                if (this.consecutiveFailures >= 3) {
                    this.healthy = false;
                }
            }
        }

        public void updateHealth(boolean isHealthy) {
            updateHealth(isHealthy, Long.MAX_VALUE);
        }

        public void markUnhealthy() {
            this.healthy = false;
            this.consecutiveFailures++;
            this.responseTime = Long.MAX_VALUE;
        }

        public long getResponseTime() {
            return responseTime;
        }

        public boolean needsSpeedTest() {
            return System.currentTimeMillis() - lastSpeedTest > 300000; // 5 минут
        }        public void markSpeedTested() {
            this.lastSpeedTest = System.currentTimeMillis();
        }

        public long getLastCheck() {
            return lastCheck;
        }
        }
    }

