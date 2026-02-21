package es.terencio.erp.shared.infrastructure.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupingConfig {

    @Bean
    public GroupedOpenApi marketingAdminApi() {
        return GroupedOpenApi.builder()
                .group("marketing-admin")
                .displayName("📣 Marketing - Admin")
                .pathsToMatch("/api/v1/companies/**/marketing/**")
                .build();
    }

    @Bean
    public GroupedOpenApi marketingPublicApi() {
        return GroupedOpenApi.builder()
                .group("marketing-public")
                .displayName("🌍 Marketing - Public")
                .pathsToMatch("/api/v1/public/marketing/**")
                .build();
    }


    @Bean
    public GroupedOpenApi customersApi() {
        return GroupedOpenApi.builder()
                .group("customers")
                .displayName("👥 Customers")
                .pathsToMatch("/api/v1/companies/**/customers/**")
                .build();
    }

   
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("auth")
                .displayName("🔐 Authentication")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("🌍 Public")
                .pathsToMatch("/api/v1/public/**")
                .build();
    }
}