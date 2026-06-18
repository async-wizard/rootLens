package com.rootlens.dashboard.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rootLensOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("rootLens API")
                        .description("Observability & root-cause analysis platform — dashboard backend")
                        .version("1.0.0"));
    }
}
