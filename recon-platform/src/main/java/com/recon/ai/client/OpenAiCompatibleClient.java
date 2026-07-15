package com.recon.ai.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.recon.ai.config.AiProperties;
import com.recon.common.enums.ResultCode;
import com.recon.common.exception.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议 LLM 客户端
 *
 * <p>适用于 DeepSeek、通义千问（DashScope 兼容模式）、Kimi（Moonshot）、OpenAI 等。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAiCompatibleClient {

    private final AiProperties properties;
    private final RestClient restClient;

    /**
     * 调用 Chat Completions
     *
     * @param model       模型名
     * @param systemPrompt 系统提示词
     * @param userPrompt  用户提示词
     * @param jsonMode    是否要求 JSON 对象输出
     * @return 聊天结果
     */
    public ChatResult chat(String model, String systemPrompt, String userPrompt, boolean jsonMode) {
        AiProperties.LlmConfig llm = properties.getLlm();
        String provider = normalizeProvider(llm.getProvider());
        AiProperties.ProviderConfig providerConfig = resolveProviderConfig(provider);

        String baseUrl = firstNonBlank(llm.getBaseUrl(),
                providerConfig != null ? providerConfig.getBaseUrl() : null,
                defaultBaseUrl(provider));
        String apiKey = firstNonBlank(llm.getApiKey(),
                providerConfig != null ? providerConfig.getApiKey() : null);
        boolean useJsonMode = jsonMode && (providerConfig == null || providerConfig.isJsonMode());

        if (StrUtil.isBlank(apiKey) || "your-api-key".equalsIgnoreCase(apiKey)) {
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                    "未配置有效的 API Key，请设置 ai.llm.api-key 或环境变量（如 DEEPSEEK_API_KEY）");
        }
        if (StrUtil.isBlank(baseUrl)) {
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "未配置 LLM Base URL");
        }
        if (StrUtil.isBlank(model)) {
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "未配置模型名称");
        }

        String url = joinUrl(baseUrl, "/chat/completions");
        Map<String, Object> body = buildRequestBody(model, systemPrompt, userPrompt, useJsonMode, llm.getTemperature());

        int maxRetries = Math.max(0, llm.getMaxRetries());
        Exception lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            long start = System.currentTimeMillis();
            try {
                String responseBody = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + apiKey)
                        .body(body)
                        .retrieve()
                        .body(String.class);
                long latency = System.currentTimeMillis() - start;
                return parseResponse(responseBody, model, provider, latency);
            } catch (RestClientException ex) {
                lastError = ex;
                log.warn("LLM 调用失败 provider={}, model={}, attempt={}/{}: {}",
                        provider, model, attempt + 1, maxRetries + 1, ex.getMessage());
            }
        }
        throw new BusinessException(ResultCode.AI_SERVICE_ERROR,
                "LLM 调用失败: " + (lastError != null ? lastError.getMessage() : "unknown"));
    }

    /**
     * 解析当前生效的模型名
     *
     * @param tier simple / default / reasoning
     * @return 模型名
     */
    public String resolveModel(String tier) {
        AiProperties.LlmConfig llm = properties.getLlm();
        String provider = normalizeProvider(llm.getProvider());
        AiProperties.ProviderConfig providerConfig = resolveProviderConfig(provider);

        AiProperties.ModelTier global = llm.getModels();
        AiProperties.ModelTier local = providerConfig != null ? providerConfig.getModels() : null;

        String model = switch (tier == null ? "default" : tier.toLowerCase()) {
            case "simple" -> firstNonBlank(
                    local != null ? local.getSimple() : null,
                    global.getSimple(),
                    local != null ? local.getDefaultModel() : null,
                    global.getDefaultModel(),
                    defaultModel(provider, "simple"));
            case "reasoning" -> firstNonBlank(
                    local != null ? local.getReasoning() : null,
                    global.getReasoning(),
                    local != null ? local.getDefaultModel() : null,
                    global.getDefaultModel(),
                    defaultModel(provider, "reasoning"));
            default -> firstNonBlank(
                    local != null ? local.getDefaultModel() : null,
                    global.getDefaultModel(),
                    defaultModel(provider, "default"));
        };
        return model;
    }

    /**
     * 当前提供商展示信息
     *
     * @return 提供商信息 Map
     */
    public Map<String, Object> currentProviderInfo() {
        AiProperties.LlmConfig llm = properties.getLlm();
        String provider = normalizeProvider(llm.getProvider());
        AiProperties.ProviderConfig cfg = resolveProviderConfig(provider);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("provider", provider);
        info.put("displayName", cfg != null && StrUtil.isNotBlank(cfg.getDisplayName())
                ? cfg.getDisplayName() : providerDisplayName(provider));
        info.put("baseUrl", firstNonBlank(llm.getBaseUrl(),
                cfg != null ? cfg.getBaseUrl() : null, defaultBaseUrl(provider)));
        info.put("apiKeyConfigured", StrUtil.isNotBlank(firstNonBlank(llm.getApiKey(),
                cfg != null ? cfg.getApiKey() : null))
                && !"your-api-key".equalsIgnoreCase(firstNonBlank(llm.getApiKey(),
                cfg != null ? cfg.getApiKey() : null)));
        info.put("defaultModel", resolveModel("default"));
        info.put("simpleModel", resolveModel("simple"));
        info.put("reasoningModel", resolveModel("reasoning"));
        return info;
    }

    private Map<String, Object> buildRequestBody(String model, String systemPrompt, String userPrompt,
                                                 boolean jsonMode, double temperature) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    private ChatResult parseResponse(String responseBody, String model, String provider, long latencyMs) {
        if (StrUtil.isBlank(responseBody)) {
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "LLM 返回空响应");
        }
        JSONObject root = JSONUtil.parseObj(responseBody);
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "LLM 响应缺少 choices");
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message != null ? message.getStr("content") : null;
        if (StrUtil.isBlank(content)) {
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "LLM 响应内容为空");
        }

        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;
        JSONObject usage = root.getJSONObject("usage");
        if (usage != null) {
            promptTokens = usage.getInt("prompt_tokens");
            completionTokens = usage.getInt("completion_tokens");
            totalTokens = usage.getInt("total_tokens");
        }

        return ChatResult.builder()
                .content(content.trim())
                .model(StrUtil.blankToDefault(root.getStr("model"), model))
                .provider(provider)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .latencyMs(latencyMs)
                .build();
    }

    private AiProperties.ProviderConfig resolveProviderConfig(String provider) {
        Map<String, AiProperties.ProviderConfig> providers = properties.getLlm().getProviders();
        if (providers == null || providers.isEmpty()) {
            return null;
        }
        return providers.get(provider);
    }

    private String normalizeProvider(String provider) {
        return StrUtil.blankToDefault(provider, "mock").trim().toLowerCase();
    }

    private String defaultBaseUrl(String provider) {
        return switch (provider) {
            case "deepseek" -> "https://api.deepseek.com/v1";
            case "qwen" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
            case "kimi" -> "https://api.moonshot.cn/v1";
            case "openai" -> "https://api.openai.com/v1";
            default -> "";
        };
    }

    private String defaultModel(String provider, String tier) {
        return switch (provider) {
            case "deepseek" -> "reasoning".equals(tier) ? "deepseek-reasoner" : "deepseek-chat";
            case "qwen" -> switch (tier) {
                case "simple" -> "qwen-turbo";
                case "reasoning" -> "qwen-max";
                default -> "qwen-plus";
            };
            case "kimi" -> "reasoning".equals(tier) ? "moonshot-v1-32k" : "moonshot-v1-8k";
            case "openai" -> "reasoning".equals(tier) || "default".equals(tier) ? "gpt-4o" : "gpt-4o-mini";
            default -> "";
        };
    }

    private String providerDisplayName(String provider) {
        return switch (provider) {
            case "deepseek" -> "DeepSeek";
            case "qwen" -> "通义千问 (Qwen)";
            case "kimi" -> "Kimi (Moonshot)";
            case "openai" -> "OpenAI";
            case "custom" -> "自定义 OpenAI 兼容接口";
            case "mock" -> "本地 Mock";
            default -> provider;
        };
    }

    private String joinUrl(String baseUrl, String path) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + path;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Chat 调用结果
     */
    @Data
    @Builder
    public static class ChatResult {

        private String content;

        private String model;

        private String provider;

        private Integer promptTokens;

        private Integer completionTokens;

        private Integer totalTokens;

        private Long latencyMs;
    }
}
