package com.recon.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka 可选启用：默认关闭自动配置，本地无 Kafka 时不影响启动与业务。
 *
 * <p>开启方式：{@code spring.kafka.enabled=true}</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class KafkaOptionalEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String KAFKA_ENABLED_KEY = "spring.kafka.enabled";
    private static final String EXCLUDE_KEY = "spring.autoconfigure.exclude";
    private static final String KAFKA_AUTO_CONFIG = KafkaAutoConfiguration.class.getName();
    private static final String HEALTH_KAFKA_KEY = "management.health.kafka.enabled";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean kafkaEnabled = environment.getProperty(KAFKA_ENABLED_KEY, Boolean.class, false);
        if (kafkaEnabled) {
            return;
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put(EXCLUDE_KEY, mergeExclude(environment.getProperty(EXCLUDE_KEY), KAFKA_AUTO_CONFIG));
        // 关闭 Kafka 健康检查，避免 actuator 因连不上 Kafka 报 DOWN
        if (environment.getProperty(HEALTH_KAFKA_KEY) == null) {
            props.put(HEALTH_KAFKA_KEY, false);
        }
        environment.getPropertySources().addFirst(
                new MapPropertySource("kafkaOptionalDefaults", props));
    }

    private String mergeExclude(String existing, String toAdd) {
        List<String> excludes = new ArrayList<>();
        if (StringUtils.hasText(existing)) {
            Arrays.stream(existing.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(excludes::add);
        }
        if (!excludes.contains(toAdd)) {
            excludes.add(toAdd);
        }
        return String.join(",", excludes);
    }
}
