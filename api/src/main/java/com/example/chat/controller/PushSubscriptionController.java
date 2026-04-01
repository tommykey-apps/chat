package com.example.chat.controller;

import com.example.chat.model.dto.PushSubscriptionRequest;
import com.example.chat.model.entity.PushSubscription;
import com.example.chat.repository.PushSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/push")
public class PushSubscriptionController {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final String vapidPublicKey;

    public PushSubscriptionController(
            PushSubscriptionRepository pushSubscriptionRepository,
            @Value("${app.webpush.vapid-public-key:}") String vapidPublicKey) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.vapidPublicKey = vapidPublicKey;
    }

    @GetMapping("/vapid-key")
    public Map<String, String> getVapidKey() {
        return Map.of("publicKey", vapidPublicKey);
    }

    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionRequest request, Principal principal) {
        var existing = pushSubscriptionRepository.findByEndpoint(request.endpoint());
        if (existing.isPresent()) {
            return ResponseEntity.ok().build();
        }

        var sub = new PushSubscription();
        sub.setUserId(principal.getName());
        sub.setEndpoint(request.endpoint());
        sub.setP256dh(request.p256dh());
        sub.setAuth(request.auth());
        pushSubscriptionRepository.save(sub);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unsubscribe")
    @Transactional
    public ResponseEntity<Void> unsubscribe(@RequestBody Map<String, String> body, Principal principal) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) {
            pushSubscriptionRepository.deleteByEndpoint(endpoint);
        }
        return ResponseEntity.noContent().build();
    }
}
