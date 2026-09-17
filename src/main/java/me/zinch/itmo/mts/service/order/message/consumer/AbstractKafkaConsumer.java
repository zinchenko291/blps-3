package me.zinch.itmo.mts.service.order.message.consumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import me.zinch.itmo.mts.config.KafkaProperties;

@Slf4j
abstract class AbstractKafkaConsumer<T> {

    private final KafkaProperties properties;
    protected final ObjectMapper objectMapper;
    private final String topic;
    private final String groupId;
    private final Class<T> messageType;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running;
    private KafkaConsumer<String, String> consumer;

    protected AbstractKafkaConsumer(KafkaProperties properties, ObjectMapper objectMapper,
            String topic, String groupId, Class<T> messageType) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.groupId = groupId;
        this.messageType = messageType;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (running) {
            return;
        }
        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.bootstrapServers());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, properties.clientId() + "-" + groupId);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(consumerProperties);
        consumer.subscribe(List.of(topic));
        running = true;
        executor.execute(this::consume);
        log.info("Kafka consumer started: topic={}, group={}", topic, groupId);
    }

    private void consume() {
        try {
            while (running) {
                try {
                    ConsumerRecords<String, String> records = consumer
                            .poll(Duration.ofMillis(properties.pollTimeoutMs()));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            handle(objectMapper.readValue(record.value(), messageType));
                            consumer.commitSync();
                        } catch (Exception e) {
                            consumer.seek(new TopicPartition(record.topic(), record.partition()), record.offset());
                            log.error("Kafka task failed and will be retried: topic={}, offset={}",
                                    record.topic(), record.offset(), e);
                            break;
                        }
                    }
                } catch (Exception e) {
                    if (running) {
                        log.error("Kafka consumer failure: topic={}", topic, e);
                    }
                }
            }
        } finally {
            consumer.close(Duration.ofSeconds(10));
        }
    }

    protected abstract void handle(T message);

    @PreDestroy
    public synchronized void stop() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
        }
        executor.shutdownNow();
    }

}
