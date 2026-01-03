package invoice;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${server.port:8089}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        // Define the JWT Bearer authentication scheme
        SecurityScheme bearerAuth = new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token");

        // Define cookie-based authentication for access token
        SecurityScheme cookieAuth = new SecurityScheme()
                .name("cookieAuth")
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name("accessToken")
                .description("JWT token stored in cookie");

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerAuth)
                        .addSecuritySchemes("cookieAuth", cookieAuth))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .addSecurityItem(new SecurityRequirement().addList("cookieAuth"))
                .info(new Info()
                        .title("Original Invoice API")
                        .description("Original Invoice API - A comprehensive invoicing solution for businesses")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Original Invoice Team")
                                .email("Admin@originalinvoice.com")))
                .servers(List.of(new Server().url("http://localhost:" + serverPort)));
    }
}