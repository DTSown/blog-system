package com.son.blog.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConfig {

    public static final String POST_LIKES_TOPIC = "post-likes-topic";

    @org.springframework.beans.factory.annotation.Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public NewTopic postLikesTopic() {
        return TopicBuilder.name(POST_LIKES_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public org.springframework.kafka.core.ProducerFactory<Object, Object> producerFactory() {
        java.util.Map<String, Object> configProps = new java.util.HashMap<>();
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        configProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        return new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public org.springframework.kafka.core.KafkaTemplate<Object, Object> kafkaTemplate() {
        return new org.springframework.kafka.core.KafkaTemplate<>(producerFactory());
    }

    @Bean
    public org.springframework.kafka.core.ConsumerFactory<Object, Object> consumerFactory() {
        java.util.Map<String, Object> props = new java.util.HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "blog-group");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonDeserializer.class);
        props.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory() {
        org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
