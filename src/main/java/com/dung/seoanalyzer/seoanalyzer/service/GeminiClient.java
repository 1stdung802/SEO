package com.dung.seoanalyzer.seoanalyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gọi Gemini API (endpoint generateContent) qua HTTP.
 * <p>
 * Cấu hình trong application.properties / secret.properties:
 * <ul>
 *   <li>gemini.api.key: API key lấy từ Google AI Studio (đặt trong secret.properties)</li>
 *   <li>gemini.model: tên model, mặc định gemini-3.8-flash (thuộc nhóm miễn phí)</li>
 *   <li>gemini.timeout-seconds: thời gian chờ tối đa, mặc định 60 giây</li>
 * </ul>
 * Không dùng thư viện SDK riêng: chỉ cần RestClient có sẵn trong Spring.
 */
@Service
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public GeminiClient(@Value("${gemini.api.key:}") String apiKey,
                        @Value("${gemini.model:gemini-3.8-flash}") String model,
                        @Value("${gemini.timeout-seconds:60}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.model = model;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public String getModel() {
        return model;
    }

    /**
     * Gửi yêu cầu tới Gemini và trả về đoạn văn bản sinh ra.
     *
     * @param systemInstruction vai trò và quy tắc chung cho AI
     * @param prompt            nội dung yêu cầu cụ thể
     */
    public String generate(String systemInstruction, String prompt) {
        if (apiKey.isEmpty()) {
            throw new AiServiceException(
                    "Chưa cấu hình gemini.api.key trong secret.properties.", 503);
        }

        Map<String, Object> request = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))))
        );

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            throw translateHttpError(e);
        } catch (ResourceAccessException e) {
            throw new AiServiceException(
                    "Không kết nối được tới Gemini hoặc quá thời gian chờ. Hãy thử lại.", 504, e);
        }

        return extractText(response);
    }

    /** Đổi mã lỗi HTTP của Gemini thành thông báo dễ hiểu. */
    private AiServiceException translateHttpError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String message = switch (status) {
            case 429 -> "Đã vượt hạn mức miễn phí của Gemini API. Hãy đợi khoảng 1 phút rồi thử lại.";
            case 400 -> "Yêu cầu không hợp lệ hoặc API key sai. Kiểm tra lại gemini.api.key.";
            case 401, 403 -> "API key không có quyền truy cập. Kiểm tra lại key trong Google AI Studio.";
            case 404 -> "Không tìm thấy model '" + model + "'. Kiểm tra lại gemini.model.";
            default -> "Gemini API trả về lỗi " + status + ". Hãy thử lại sau.";
        };
        return new AiServiceException(message, status == 429 ? 429 : 502, e);
    }

    /**
     * Lấy văn bản từ phản hồi: candidates[0].content.parts[*].text.
     * Bỏ qua các phần "suy nghĩ" nội bộ của model (thought = true) nếu có.
     */
    @SuppressWarnings("unchecked")
    static String extractText(Map<String, Object> response) {
        if (response == null) {
            throw new AiServiceException("Gemini không trả về dữ liệu.", 502);
        }

        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            Object feedback = response.get("promptFeedback");
            String reason = feedback instanceof Map<?, ?> f && f.get("blockReason") != null
                    ? " (lý do: " + f.get("blockReason") + ")" : "";
            throw new AiServiceException("Gemini không sinh được nội dung" + reason + ".", 502);
        }

        Map<String, Object> first = (Map<String, Object>) candidates.get(0);
        Object contentObj = first.get("content");
        if (!(contentObj instanceof Map<?, ?> content) || !(content.get("parts") instanceof List<?> parts)) {
            throw new AiServiceException("Phản hồi của Gemini không có nội dung văn bản.", 502);
        }

        StringBuilder text = new StringBuilder();
        for (Object partObj : parts) {
            if (partObj instanceof Map<?, ?> part
                    && !Boolean.TRUE.equals(part.get("thought"))
                    && part.get("text") instanceof String s) {
                text.append(s);
            }
        }
        if (text.isEmpty()) {
            throw new AiServiceException("Phản hồi của Gemini không có nội dung văn bản.", 502);
        }
        return text.toString().strip();
    }
}
