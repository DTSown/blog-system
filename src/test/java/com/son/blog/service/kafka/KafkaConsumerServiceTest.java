package com.son.blog.service.kafka;

import com.son.blog.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    private Map<String, Object> event;

    @BeforeEach
    void setUp() {
        event = new HashMap<>();
        event.put("postId", 1L);
        event.put("likedByUsername", "john_doe");
        event.put("authorEmail", "author@example.com");
        event.put("postTitle", "My Awesome Post");
    }

    @Test
    void testConsumeLikeEvent_Success() {
        // Run the consumer method
        kafkaConsumerService.consumeLikeEvent(event);

        // Verify that EmailService was called once with the correct parameters
        verify(emailService, times(1)).sendLikeNotification(
                "author@example.com",
                "My Awesome Post",
                "john_doe"
        );
    }

    @Test
    void testConsumeLikeEvent_MissingEmail() {
        // Remove email to simulate missing data
        event.put("authorEmail", null);

        kafkaConsumerService.consumeLikeEvent(event);

        // Verify that EmailService is NOT called
        verify(emailService, never()).sendLikeNotification(anyString(), anyString(), anyString());
    }

    @Test
    void testConsumeLikeEvent_EmptyEmail() {
        // Use empty string
        event.put("authorEmail", "");

        kafkaConsumerService.consumeLikeEvent(event);

        // Verify that EmailService is NOT called
        verify(emailService, never()).sendLikeNotification(anyString(), anyString(), anyString());
    }
}
