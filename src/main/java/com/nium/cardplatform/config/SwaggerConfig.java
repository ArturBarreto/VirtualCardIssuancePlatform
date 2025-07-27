package com.nium.cardplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI virtualCardOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Virtual Card Issuance API")
                        .version("1.0.0")
                        .description("A Spring Boot backend system for managing virtual cards, implementing creation, top-up, spending, blocking/unblocking, and transaction history retrieval.\n" +
                                "\n" +
                                "This solution uses JOOQ for database access, H2 for DB, full RESTful APIs, custom exception handling, and robust unit & integration tests.")
                        .contact(new Contact()
                                .name("Artur Gomes Barreto")
                                .email("artur.gomes.barreto@gmail.com")
                                .url("https://www.linkedin.com/in/arturgomesbarreto/")
                        )
                );
    }
}
