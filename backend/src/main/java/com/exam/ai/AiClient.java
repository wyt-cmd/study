package com.exam.ai;

import com.exam.common.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AiClient {

    private final boolean enabled;
    private final String model;
    private final String apiKey;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AiClient(@Value("${app.ai.enabled:false}") boolean enabled,
                    @Value("${app.ai.base-url}") String baseUrl,
                    @Value("${app.ai.api-key:}") String apiKey,
                    @Value("${app.ai.model}") String model,
                    ObjectMapper objectMapper) {
        this.enabled = enabled && StringUtils.hasText(apiKey);
        this.model = model;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public boolean enabled() {
        return enabled;
    }

    public JsonNode chat(String prompt) {
        if (!enabled) {
            throw new BizException("未配置 AI 密钥，已拒绝调用模型");
        }
        try {
            String body = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "model", model,
                            "temperature", 0.2,
                            "messages", List.of(Map.of("role", "user", "content", prompt))
                    ))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(40));
            JsonNode root = objectMapper.readTree(body);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            return extractJson(content);
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("AI 调用失败: {}", ex.getMessage());
            throw new BizException("AI 调用失败: " + ex.getMessage());
        }
    }

    public Optional<JsonNode> tryChat(String prompt) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            return Optional.of(chat(prompt));
        } catch (RuntimeException ex) {
            log.warn("AI 降级为规则评分: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private JsonNode extractJson(String content) throws Exception {
        int obj = content.indexOf('{');
        int arr = content.indexOf('[');
        int start;
        char end;
        if (obj < 0 && arr < 0) {
            throw new BizException("模型没有返回 JSON");
        }
        if (arr >= 0 && (obj < 0 || arr < obj)) {
            start = arr;
            end = ']';
        } else {
            start = obj;
            end = '}';
        }
        int finish = content.lastIndexOf(end);
        return objectMapper.readTree(content.substring(start, finish + 1));
    }
}
