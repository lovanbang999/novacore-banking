package com.novacore.banking.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cusOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Novacore Banking API")
                .version("1.0.0")
                .description("Core Banking Engine RESTful APIs for Customer Onboarding, Account Management, and KYC Verification.")
                .contact(new Contact()
                    .name("NovaCore Engineering Team")
                    .email("support@novacore.bank"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://springdoc.org")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local Development Server")));
    }
}
