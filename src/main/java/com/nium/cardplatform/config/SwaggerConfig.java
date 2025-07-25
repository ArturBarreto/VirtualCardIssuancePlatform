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
                        .description("REST API for virtual card creation, balance management, and secure card transactions. Includes business rules, error handling, and optimistic concurrency.")
                        .contact(new Contact()
                                .name("Artur Gomes Barreto")
                                .email("artur.gomes.barreto@gmail.com")
                                .url("https://www.linkedin.com/in/arturgomesbarreto/")
                        )
                );
    }
}
