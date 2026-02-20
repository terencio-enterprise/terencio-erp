package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@Profile("prod")
public class AwsConfig {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder().region(Region.EU_WEST_1).credentialsProvider(DefaultCredentialsProvider.create()).build();
    }

    @Bean
    public SesClient sesClient() {
        return SesClient.builder().region(Region.EU_WEST_1).credentialsProvider(DefaultCredentialsProvider.create()).build();
    }
}