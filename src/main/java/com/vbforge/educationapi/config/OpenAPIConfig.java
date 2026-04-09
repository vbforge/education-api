package com.vbforge.educationapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI educationAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Education API")
                        .description("Educational Management System REST API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("vbforge")
                                .email("support@vbforge.com")
                                .url("https://vbforge.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.education.com").description("Production")
                ));
    }
}