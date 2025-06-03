package src.main.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private volatile boolean initialized = false;

    public void initialize() {
        if (!proxyEnabled || initialized) {
            return;
        }

        try {
            parseProxyUrls();
            if (rotationEnabled) {
                startHealthCheck();
                startProxyRotation();
            }
            setupAuthenticator();
            initialized = true;
            log.info("ProxyService initialized with {} proxies", proxyList.size());
        } catch (Exception e) {
            log.error("Failed to initialize ProxyService: {}", e.getMessage(), e);
        }
    }

    private void parseProxyUrls() {
        if (proxyUrls == null || proxyUrls.trim().isEmpty()) {
            log.warn("No proxy URLs configured");
            return;
        }

        String[] urls = proxyUrls.split(",");
        for (String url : urls) {
            try {
                URI uri = URI.create(url.trim());
                String host = uri.getHost();
                int port = uri.getPort();
                
                if (host != null && port > 0) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
                    proxyList.add(proxy);
                    proxyHealth.put(proxy, new ProxyHealth());
                    log.info("Added proxy: {}:{}", host, port);
                }
            } catch (Exception e) {
                log.warn("Invalid proxy URL: {}, error: {}", url, e.getMessage());
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
    }

    private void setupAuthenticator() {
        if (proxyUser != null && !proxyUser.isEmpty() && 
            proxyPassword != null && !proxyPassword.isEmpty()) {
            
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        log.debug("Providing proxy authentication");
                        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                    }
                    return null;
                }
            });
        }
    }

    private void startHealthCheck() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                checkProxyHealth();
            } catch (Exception e) {
                log.error("Error during proxy health check: {}", e.getMessage());
            }
        }, 30, 60, TimeUnit.SECONDS);
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
            // Use a simple HTTP endpoint for testing to avoid SSL issues
            URL testUrl = new URL("http://httpbin.org/ip");
            HttpURLConnection connection = (HttpURLConnection) testUrl.openConnection(proxy);
            connection.setConnectTimeout(5000); // Reduced timeout to 5 seconds
            connection.setReadTimeout(5000);    // Reduced timeout to 5 seconds
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            boolean isHealthy = responseCode >= 200 && responseCode < 400;
            if (isHealthy) {
                log.info("Proxy {} health check successful (response: {})", proxy.address(), responseCode);
            } else {
                log.warn("Proxy {} health check failed with response code: {}", proxy.address(), responseCode);
            }
            return isHealthy;
        } catch (Exception e) {
            log.warn("Proxy {} health check failed: {}", proxy.address(), e.getMessage());
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
    }

    public Proxy getCurrentProxy() {
        if (!proxyEnabled || !initialized) {
            return Proxy.NO_PROXY;
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

        int index = currentProxyIndex.get() % healthyProxies.size();
        return healthyProxies.get(index);
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
            private final ProxySelector defaultSelector = ProxySelector.getDefault();

            @Override
            public List<Proxy> select(URI uri) {
                // Check if this is a request to Google APIs
                if (uri.getHost() != null && 
                    (uri.getHost().contains("googleapis.com") || 
                     uri.getHost().contains("generativelanguage.googleapis.com"))) {
                    
                    Proxy currentProxy = getCurrentProxy();
                    if (currentProxy != Proxy.NO_PROXY) {
                        log.debug("Routing request to {} through proxy: {}", uri.getHost(), currentProxy.address());
                        return List.of(currentProxy);
                    }
                }
                
                // For all other requests, use default selector
                return defaultSelector != null ? defaultSelector.select(uri) : List.of(Proxy.NO_PROXY);
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
    }

    public List<String> getProxyStatus() {
        List<String> status = new ArrayList<>();
        status.add(String.format("Proxy Service: %s", proxyEnabled ? "Enabled" : "Disabled"));
        status.add(String.format("Total Proxies: %d", proxyList.size()));
        status.add(String.format("Healthy Proxies: %d", getHealthyProxies().size()));
        
        for (int i = 0; i < proxyList.size(); i++) {
            Proxy proxy = proxyList.get(i);
            ProxyHealth health = proxyHealth.get(proxy);
            String healthStatus = health != null ? (health.isHealthy() ? "Healthy" : "Unhealthy") : "Unknown";
            boolean isCurrent = i == (currentProxyIndex.get() % proxyList.size());
            status.add(String.format("Proxy %d: %s [%s] %s", 
                i + 1, proxy.address(), healthStatus, isCurrent ? "(Current)" : ""));
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
    }

    public void checkAllProxiesHealth() {
        log.info("Manually triggering health check for all proxies");
        checkProxyHealth();
    }

    private static class ProxyHealth {
        private volatile boolean healthy = true;
        private volatile long lastCheck = System.currentTimeMillis();
        private volatile int consecutiveFailures = 0;

        public boolean isHealthy() {
            return healthy && consecutiveFailures < 3;
        }

        public void updateHealth(boolean isHealthy) {
            this.lastCheck = System.currentTimeMillis();
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

        public void markUnhealthy() {
            this.healthy = false;
            this.consecutiveFailures++;
        }
    }
}
