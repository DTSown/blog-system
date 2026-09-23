package com.son.blog.service.kafka;

import com.son.blog.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public void sendLikeEvent(Long postId, String likedByUsername, String authorEmail, String postTitle) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> event = new HashMap<>();
                event.put("postId", postId);
                event.put("likedByUsername", likedByUsername);
                event.put("authorEmail", authorEmail);
                event.put("postTitle", postTitle);

                kafkaTemplate.send(KafkaConfig.POST_LIKES_TOPIC, String.valueOf(postId), event);
            } catch (Exception e) {
                // Log error but do not fail the transaction if Kafka is down
                System.err.println("Failed to send Kafka like event: " + e.getMessage());
            }
        });
    }
}
