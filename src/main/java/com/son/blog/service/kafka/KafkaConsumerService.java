package com.son.blog.service.kafka;

import com.son.blog.config.KafkaConfig;
import com.son.blog.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final EmailService emailService;

    @KafkaListener(topics = KafkaConfig.POST_LIKES_TOPIC, groupId = "blog-group")
    public void consumeLikeEvent(Map<String, Object> event) {
        log.info("Received Like Event from Kafka: {}", event);

        String authorEmail = (String) event.get("authorEmail");
        String likedByUsername = (String) event.get("likedByUsername");
        String postTitle = (String) event.get("postTitle");

        if (authorEmail != null && !authorEmail.isEmpty()) {
            try {
                emailService.sendLikeNotification(authorEmail, postTitle, likedByUsername);
                log.info("Successfully sent like notification email to {}", authorEmail);
            } catch (Exception e) {
                log.error("Failed to send email to {}", authorEmail, e);
            }
        }
    }
}
