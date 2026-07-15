package com.recon.ai.config;

import com.recon.ai.client.OpenAiCompatibleClient;
import com.recon.ai.service.AIService;
import com.recon.ai.service.LlmAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 自动配置：注册属性、HTTP 客户端与生产环境 LLM 实现
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiAutoConfiguration {

    /**
     * 生产 LLM 服务 Bean（bean 名 productionAIService）
     *
     * <p>当 ai.llm.provider 不为 mock 时启用，MockAIService 因
     * ConditionalOnMissingBean 自动让出。</p>
     *
     * @param properties AI 配置
     * @param restClient LLM RestClient
     * @return AIService 实现
     */
    @Bean(name = "productionAIService")
    @ConditionalOnExpression("!'${ai.llm.provider:mock}'.equalsIgnoreCase('mock')")
    public AIService productionAIService(AiProperties properties, RestClient.Builder restClientBuilder) {
        String provider = properties.getLlm().getProvider();
        log.info("启用生产 LLM 服务, provider={}", provider);
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(properties, buildRestClient(properties, restClientBuilder));
        return new LlmAIService(properties, client);
    }

    private RestClient buildRestClient(AiProperties properties, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getLlm().getTimeoutMs());
        factory.setReadTimeout(properties.getLlm().getTimeoutMs());
        return builder.requestFactory(factory).build();
    }
}
