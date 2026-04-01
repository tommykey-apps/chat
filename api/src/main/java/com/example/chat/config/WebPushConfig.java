package com.example.chat.config;

import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
public class WebPushConfig {

    @Bean
    @ConditionalOnExpression("!'${app.webpush.vapid-public-key:}'.isEmpty()")
    public PushService pushService(
            @Value("${app.webpush.vapid-public-key}") String publicKey,
            @Value("${app.webpush.vapid-private-key}") String privateKey,
            @Value("${app.webpush.vapid-subject:mailto:admin@example.com}") String subject)
            throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        return new PushService(publicKey, privateKey, subject);
    }
}
