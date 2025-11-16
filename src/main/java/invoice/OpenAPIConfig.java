package invoice;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        // Define the JWT Bearer authentication scheme
        SecurityScheme bearerAuth = new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("Original Invoice API")
                        .description("Original Invoice API Documentation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Joseph")
                                .email("teresejosephyisa@gmail.com")
                        )
                )
                .schemaRequirement("bearerAuth", bearerAuth)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
