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

import java.util.List;

@Configuration
@Slf4j
public class OpenApiConfig {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${app.server.url}")
    private String serverUrl;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${springdoc.api-docs.path}")
    private String apiDocsPath;

    @Value("${springdoc.swagger-ui.path}")
    private String swaggerPath;

    @Bean
    public OpenAPI openAPI() {
        String fullServerUrl = serverUrl + contextPath;
        log.info("Configuring OpenAPI for server URL: {}", fullServerUrl);

        try {
            return new OpenAPI()
                    .info(new Info()
                            .title(appName + " API")
                            .description("API для приложения управления личными финансами " + appName)
                            .version(appVersion)
                            .contact(new Contact()
                                    .name(appName + " Team")
                                    .email("support@example.com")
                                    .url("https://example.com"))
                            .license(new License()
                                    .name("Specify License")
                                    .url("Specify License URL")))
                    .servers(List.of(
                            new Server()
                                    .url(fullServerUrl)
                                    .description("Server for the current environment (" + getActiveProfileDescription() + ")")))
                    .components(new Components()
                            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("JWT токен авторизации. Пример: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")))
                    .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        } catch (Exception e) {
            log.error("Error configuring OpenAPI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure OpenAPI", e);
        }
    }

    private String getActiveProfileDescription() {
        String activeProfiles = System.getProperty("spring.profiles.active");
        if (activeProfiles != null && !activeProfiles.isEmpty()) {
            return activeProfiles;
        }
        return "default";
    }
} 