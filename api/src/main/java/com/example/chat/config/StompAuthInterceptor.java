package com.example.chat.config;

import com.example.chat.service.PresenceService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final PresenceService presenceService;

    public StompAuthInterceptor(JwtDecoder jwtDecoder, PresenceService presenceService) {
        this.jwtDecoder = jwtDecoder;
        this.presenceService = presenceService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String token = authHeaders.getFirst();
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }

                var jwt = jwtDecoder.decode(token);
                String sub = jwt.getSubject();

                var authentication = new UsernamePasswordAuthenticationToken(
                        sub, null, List.of());
                accessor.setUser(authentication);
                presenceService.setOnline(sub);
            }
        }

        if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            var user = accessor.getUser();
            if (user != null) {
                presenceService.setOffline(user.getName());
            }
        }

        return message;
    }
}
