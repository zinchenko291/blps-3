package me.zinch.itmo.mts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record KafkaProperties(String bootstrapServers, int pollTimeoutMs, String clientId) {
}
