package me.zinch.itmo.mts.service.order.message.producer;

import java.time.Duration;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import me.zinch.itmo.mts.config.KafkaProperties;
import me.zinch.itmo.mts.service.ServiceException;

@Component
@Slf4j
public class KafkaMessageProducer {

    private final ObjectMapper objectMapper;
    private final KafkaProducer<String, String> producer;

    public KafkaMessageProducer(ObjectMapper objectMapper, KafkaProperties properties) {
        this.objectMapper = objectMapper;
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, properties.clientId() + "-producer");
        producerProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(producerProperties);
    }

    public void send(String topic, String key, Object message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            producer.send(new ProducerRecord<>(topic, key, payload)).get();
            log.info("Kafka message sent: topic={}, key={}", topic, key);
        } catch (JsonProcessingException e) {
            throw new ServiceException("Не удалось сериализовать Kafka-сообщение", e);
        } catch (Exception e) {
            throw new ServiceException("Не удалось отправить Kafka-сообщение в " + topic, e);
        }
    }

    @PreDestroy
    void close() {
        producer.close(Duration.ofSeconds(10));
    }
}
