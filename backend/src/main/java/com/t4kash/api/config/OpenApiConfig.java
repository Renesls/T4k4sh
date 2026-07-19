package com.t4kash.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI t4kashOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("T4KASH API")
                        .version("0.1.0")
                        .description("API REST para oportunidades universitarias, postulaciones y entregas."));
    }
}
