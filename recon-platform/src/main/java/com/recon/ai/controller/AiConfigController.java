package com.recon.ai.controller;

import cn.hutool.core.util.StrUtil;
import com.recon.ai.client.OpenAiCompatibleClient;
import com.recon.ai.config.AiProperties;
import com.recon.ai.service.AIService;
import com.recon.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI / 大模型配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI大模型配置")
public class AiConfigController {

    private final AiProperties aiProperties;
    private final AIService aiService;
    private final RestClient.Builder restClientBuilder;

    @Operation(summary = "查询当前 AI 配置状态")
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getConfig() {
        String provider = StrUtil.blankToDefault(aiProperties.getLlm().getProvider(), "mock").toLowerCase();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", provider);
        result.put("mockMode", "mock".equalsIgnoreCase(provider));
        result.put("implementation", aiService.getClass().getSimpleName());

        if ("mock".equalsIgnoreCase(provider)) {
            result.put("displayName", "本地 Mock");
            result.put("apiKeyConfigured", false);
            result.put("defaultModel", "mock-ai-v1.0");
            result.put("simpleModel", "mock-ai-v1.0");
            result.put("reasoningModel", "mock-ai-v1.0");
            result.put("baseUrl", "");
            result.put("message", "当前为 Mock 模式，修改 ai.llm.provider 为 deepseek/qwen/kimi 并配置 API Key 后重启生效");
        } else {
            OpenAiCompatibleClient client = buildClient();
            result.putAll(client.currentProviderInfo());
            result.put("message", "已接入国内/国际大模型 OpenAI 兼容接口");
        }
        result.put("supportedProviders", listProviders());
        return ApiResponse.success(result);
    }

    @Operation(summary = "查询支持的大模型提供商")
    @GetMapping("/providers")
    public ApiResponse<List<Map<String, Object>>> providers() {
        return ApiResponse.success(listProviders());
    }

    @Operation(summary = "测试大模型连通性")
    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> test(@RequestBody(required = false) Map<String, String> body) {
        String provider = StrUtil.blankToDefault(aiProperties.getLlm().getProvider(), "mock").toLowerCase();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", provider);

        if ("mock".equalsIgnoreCase(provider)) {
            result.put("success", true);
            result.put("message", "Mock 模式无需外网连通，接口可用");
            result.put("sample", "Mock AI 服务运行正常");
            return ApiResponse.success(result);
        }

        try {
            OpenAiCompatibleClient client = buildClient();
            String model = client.resolveModel("simple");
            String prompt = body != null && StrUtil.isNotBlank(body.get("prompt"))
                    ? body.get("prompt")
                    : "请用一句话介绍你自己，并说明你可用于财务对账场景。返回 JSON：{\"reply\":\"...\"}";
            long start = System.currentTimeMillis();
            OpenAiCompatibleClient.ChatResult chat = client.chat(
                    model,
                    "你是智能对账平台接入的大模型，只输出 JSON。",
                    prompt,
                    true);
            result.put("success", true);
            result.put("model", chat.getModel());
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("tokensUsed", chat.getTotalTokens());
            result.put("content", chat.getContent());
            result.put("message", "连通性测试成功");
        } catch (Exception ex) {
            log.warn("大模型连通性测试失败: {}", ex.getMessage());
            result.put("success", false);
            result.put("message", ex.getMessage());
        }
        return ApiResponse.success(result);
    }

    private OpenAiCompatibleClient buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(aiProperties.getLlm().getTimeoutMs());
        factory.setReadTimeout(aiProperties.getLlm().getTimeoutMs());
        RestClient restClient = restClientBuilder.requestFactory(factory).build();
        return new OpenAiCompatibleClient(aiProperties, restClient);
    }

    private List<Map<String, Object>> listProviders() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(providerItem("mock", "本地 Mock", "", "无需 Key，开发演示"));
        list.add(providerItem("deepseek", "DeepSeek", "https://api.deepseek.com/v1",
                "环境变量 DEEPSEEK_API_KEY，模型 deepseek-chat / deepseek-reasoner"));
        list.add(providerItem("qwen", "通义千问 (Qwen)", "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "环境变量 DASHSCOPE_API_KEY，模型 qwen-turbo / qwen-plus / qwen-max"));
        list.add(providerItem("kimi", "Kimi (Moonshot)", "https://api.moonshot.cn/v1",
                "环境变量 MOONSHOT_API_KEY，模型 moonshot-v1-8k / moonshot-v1-32k"));
        list.add(providerItem("openai", "OpenAI", "https://api.openai.com/v1",
                "环境变量 AI_API_KEY 或 OPENAI_API_KEY"));
        list.add(providerItem("custom", "自定义 OpenAI 兼容", "自行配置 base-url",
                "任意兼容 /v1/chat/completions 的国内私有化或中转网关"));
        return list;
    }

    private Map<String, Object> providerItem(String code, String name, String baseUrl, String tip) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("name", name);
        item.put("baseUrl", baseUrl);
        item.put("tip", tip);
        item.put("active", code.equalsIgnoreCase(aiProperties.getLlm().getProvider()));
        return item;
    }
}
