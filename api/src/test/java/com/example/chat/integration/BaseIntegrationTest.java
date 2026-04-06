package com.example.chat.integration;

import com.example.chat.repository.ChatMessageSearchRepository;
import com.example.chat.service.SearchService;
import com.example.chat.service.EmailNotificationService;
import com.example.chat.service.NotificationSender;
import com.example.chat.service.WebPushService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    static {
        System.setProperty("api.version", "1.43");
    }

    static final PostgreSQLContainer<?> postgres;
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("chat_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();

        redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379);
        redis.start();
    }

    @MockitoBean
    protected JwtDecoder jwtDecoder;

    @MockitoBean
    protected SearchService searchService;

    @MockitoBean
    protected NotificationSender notificationSender;

    @MockitoBean
    protected WebPushService webPushService;

    @MockitoBean
    protected EmailNotificationService emailNotificationService;

    @MockitoBean
    protected ChatMessageSearchRepository chatMessageSearchRepository;

    @MockitoBean
    protected software.amazon.awssdk.services.s3.presigner.S3Presigner s3Presigner;

    @MockitoBean
    protected software.amazon.awssdk.services.s3.S3Client s3Client;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
