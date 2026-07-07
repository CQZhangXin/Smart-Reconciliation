package com.recon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka 配置 — 仅在配置启用时激活
 *
 * 若本地无 Kafka 环境，设置 spring.kafka.enabled=false 或移除依赖
 */
@Slf4j
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaConfig {

    // Kafka 消费者/生产者配置由 spring.kafka.* 自动装配
    // 此处仅用于条件启用

    static {
        log.info("Kafka 消息队列已启用");
    }
}
