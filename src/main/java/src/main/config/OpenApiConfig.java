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

    @Bean
    public OpenAPI openAPI() {
        try {
            log.debug("Configuring OpenAPI with context path: {}", contextPath);
            
            return new OpenAPI()
                    .info(new Info()
                            .title("MoneyGuard API")
                            .description("API для приложения управления личными финансами MoneyGuard")
                            .version("1.0.0")
                            .contact(new Contact()
                                    .name("MoneyGuard Team")
                                    .email("123")
                                    .url("123"))
                            .license(new License()
                                    .name("123")
                                    .url("123")))
                    .servers(List.of(
                            new Server()
                                    .url("http://localhost:8080" + contextPath)
                                    .description("Local Development Server"),
                            new Server()
                                    .url("https://api.moneyguard.ru" + contextPath)
                                    .description("Production Server")))
                    .components(new Components()
                            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description("JWT токен авторизации. Пример: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")))
                    .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
        } catch (Exception e) {
            log.error("Error configuring OpenAPI: {}", e.getMessage(), e);
            throw e;
        }
    }
} 