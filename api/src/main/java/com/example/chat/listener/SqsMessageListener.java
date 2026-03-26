package com.example.chat.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SqsMessageListener {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageListener.class);

    @SqsListener("${app.sqs.chat-message-queue}")
    public void onMessage(String message) {
        log.info("Received SQS message: {}", message);
    }
}
