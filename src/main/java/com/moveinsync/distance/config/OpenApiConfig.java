package com.moveinsync.distance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata and a bearer-JWT security scheme, so the Swagger UI shows an "Authorize"
 * button where a token from {@code /api/auth/login} can be pasted to try the secured endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI distanceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GPS Distance Reconstruction API")
                        .version("0.1.0")
                        .description("Rebuilds a trusted, explainable billable distance for every trip "
                                + "from unreliable GPS pings. Log in at /api/auth/login to get a token."))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
