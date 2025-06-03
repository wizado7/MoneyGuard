package src.main.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import src.main.service.ProxyService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vpn-proxy")
@CrossOrigin(origins = "*")
public class VpnProxyController {

    @Autowired
    private ProxyService proxyService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getProxyStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get current proxy status
            Map<String, Boolean> proxyStatuses = proxyService.getAllProxyStatuses();
            String currentProxy = proxyService.getCurrentProxyUrl();
            int healthyCount = (int) proxyStatuses.values().stream().mapToLong(healthy -> healthy ? 1 : 0).sum();
            
            response.put("success", true);
            response.put("currentProxy", currentProxy);
            response.put("totalProxies", proxyStatuses.size());
            response.put("healthyProxies", healthyCount);
            response.put("proxyStatuses", proxyStatuses);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to get proxy status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshProxyHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Force a health check on all proxies
            proxyService.checkAllProxiesHealth();
            
            // Get updated status
            Map<String, Boolean> proxyStatuses = proxyService.getAllProxyStatuses();
            String currentProxy = proxyService.getCurrentProxyUrl();
            int healthyCount = (int) proxyStatuses.values().stream().mapToLong(healthy -> healthy ? 1 : 0).sum();
            
            response.put("success", true);
            response.put("message", "Proxy health checks refreshed");
            response.put("currentProxy", currentProxy);
            response.put("totalProxies", proxyStatuses.size());
            response.put("healthyProxies", healthyCount);
            response.put("proxyStatuses", proxyStatuses);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to refresh proxy health: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentProxy() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String currentProxy = proxyService.getCurrentProxyUrl();
            boolean isHealthy = currentProxy != null && proxyService.isProxyHealthy(currentProxy);
            
            response.put("success", true);
            response.put("currentProxy", currentProxy);
            response.put("isHealthy", isHealthy);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to get current proxy: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
