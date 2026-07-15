package com.recon.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * AI / LLM 配置属性
 *
 * <p>支持国内主流大模型（DeepSeek、通义千问 Qwen、Kimi/Moonshot 等）及 OpenAI，
 * 统一走 OpenAI 兼容 Chat Completions 协议。</p>
 */
@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private LlmConfig llm = new LlmConfig();

    private EmbeddingConfig embedding = new EmbeddingConfig();

    private SemanticMatchConfig semanticMatch = new SemanticMatchConfig();

    /**
     * LLM 主配置
     */
    @Data
    public static class LlmConfig {

        /**
         * 当前提供商: mock / deepseek / qwen / kimi / openai / custom
         */
        private String provider = "mock";

        /**
         * 全局 API Key（可被 providers.&lt;name&gt;.api-key 覆盖）
         */
        private String apiKey = "";

        /**
         * 全局 Base URL（可被 providers.&lt;name&gt;.base-url 覆盖）
         */
        private String baseUrl = "";

        private int timeoutMs = 60000;

        private int maxRetries = 2;

        private double temperature = 0.2;

        /**
         * 任务模型映射（可被 providers.&lt;name&gt;.models 覆盖）
         */
        private ModelTier models = new ModelTier();

        /**
         * 各提供商预设
         */
        private Map<String, ProviderConfig> providers = new HashMap<>();
    }

    /**
     * 模型档位
     */
    @Data
    public static class ModelTier {

        /** 通用默认模型 */
        private String defaultModel = "";

        /** 轻量分类/映射 */
        private String simple = "";

        /** 推理/根因/NL 查询 */
        private String reasoning = "";
    }

    /**
     * 单个提供商配置
     */
    @Data
    public static class ProviderConfig {

        private String displayName = "";

        private String baseUrl = "";

        private String apiKey = "";

        private ModelTier models = new ModelTier();

        /** 是否支持 JSON Object 响应格式 */
        private boolean jsonMode = true;
    }

    /**
     * Embedding 配置（预留）
     */
    @Data
    public static class EmbeddingConfig {

        private String provider = "local";

        private String model = "bge-m3";

        private int dimension = 1024;

        private boolean local = true;

        private String baseUrl = "";

        private String apiKey = "";
    }

    /**
     * 语义匹配阈值
     */
    @Data
    public static class SemanticMatchConfig {

        private double embeddingThreshold = 0.75;

        private double llmThresholdAuto = 0.85;

        private double llmThresholdRecommend = 0.70;

        private int candidateLimit = 100;

        private int fineRankLimit = 3;
    }
}
