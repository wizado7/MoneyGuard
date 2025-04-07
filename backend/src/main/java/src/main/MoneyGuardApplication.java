package src.main;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "src.main.repository")
@EntityScan(basePackages = "src.main.model")
@EnableScheduling
@EnableCaching
@Slf4j
public class MoneyGuardApplication {

    public static void main(String[] args) {
        try {
            log.info("Starting MoneyGuard application");
            ApplicationContext context = SpringApplication.run(MoneyGuardApplication.class, args);
            log.info("MoneyGuard application started successfully");
            
            // Логируем информацию о профиле
            String[] activeProfiles = context.getEnvironment().getActiveProfiles();
            if (activeProfiles.length > 0) {
                log.info("Active profiles: {}", String.join(", ", activeProfiles));
            } else {
                log.info("No active profiles, using default profile");
            }
        } catch (Exception e) {
            log.error("Error starting MoneyGuard application: {}", e.getMessage(), e);
            throw e;
        }
    }

}
