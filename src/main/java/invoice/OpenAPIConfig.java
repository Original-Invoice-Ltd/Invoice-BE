package invoice;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        // Define the security scheme (Bearer Token)
        SecurityScheme bearerAuthScheme = new SecurityScheme()
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
                // Register the security scheme
                .schemaRequirement("bearerAuth", bearerAuthScheme)

                // Apply the scheme globally (adds lock icon to all endpoints)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
