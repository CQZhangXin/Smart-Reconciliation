package com.recon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka 配置 — 仅在显式启用时激活
 *
 * <p>默认 {@code spring.kafka.enabled=false}，本地无 Kafka 也可正常启动业务。
 * 需要消息队列时设置 {@code spring.kafka.enabled=true} 并保证 bootstrap-servers 可达。</p>
 */
@Slf4j
@Configuration
@EnableKafka
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    static {
        log.info("Kafka 消息队列已启用");
    }
}
