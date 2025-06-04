package src.main.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/v1}")
    private String contextPath;

    @Value("${app.server.url:http://localhost:8080}")
    private String serverUrl;

    @Value("${spring.application.name:MoneyGuard}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    private String apiDocsPath;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerPath;

    private final Environment environment;

    public OpenApiConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public OpenAPI openAPI() {
        String activeProfile = getActiveProfileDescription();
        
        // Определяем серверы в зависимости от профиля
        List<Server> servers;
        if ("dev".equals(activeProfile)) {
            servers = List.of(
                new Server()
                    .url("https://moneyguard.asuscomm.com/v1")
                    .description("Production server (HTTPS)"),
                new Server()
                    .url("http://localhost:8080/v1")
                    .description("Local development server")
            );
        } else {
            String fullServerUrl = serverUrl + contextPath;
            servers = List.of(
                new Server()
                    .url(fullServerUrl)
                    .description("Server for the current environment (" + activeProfile + ")")
            );
        }
        
        log.info("Configuring OpenAPI with servers: {}", servers);

        try {
            return new OpenAPI()
                    .info(new Info()
                            .title(appName + " API")
                            .description("API для приложения управления личными финансами " + appName)
                            .version(appVersion)
                            .contact(new Contact()
                                    .name(appName + " Team")
                                    .email("support@moneyguard.com")
                                    .url("https://moneyguard.asuscomm.com"))
                            .license(new License()
                                    .name("MIT License")
                                    .url("https://opensource.org/licenses/MIT")))
                    .servers(servers)
                    .components(new Components()
                            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("JWT токен авторизации. Получите токен через /auth/login")))
                    .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        } catch (Exception e) {
            log.error("Error configuring OpenAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure OpenAPI", e);
        }
    }

    private String getActiveProfileDescription() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[0];
        }
        return "default";
    }
}
