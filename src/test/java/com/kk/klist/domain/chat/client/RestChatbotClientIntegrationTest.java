package com.kk.klist.domain.chat.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kk.klist.domain.chat.config.ChatbotClientConfig;
import com.kk.klist.domain.chat.domain.exception.ChatErrorCode;
import com.kk.klist.domain.chat.domain.exception.ChatException;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotQueryRequest;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotQueryResponse;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotResponseStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RestChatbotClientIntegrationTest {

    private static final String RESPONSE_BODY = "{\"status\":\"COMPLETED\",\"answer\":\"통합 응답\"}";

    private HttpServer server;
    private final AtomicReference<Response> response = new AtomicReference<>();
    private final AtomicReference<CapturedRequest> capturedRequest = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/chat/query", this::handleQuery);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("실제 HTTP 호출이 성공하면 인증 키와 trace ID를 전달하고 응답을 변환한다")
    void query_whenHttpCallSucceeds_sendsHeadersAndReturnsResponse() {
        // given
        response.set(new Response(200, RESPONSE_BODY, 0));
        RestChatbotClient client = createClient(1000);

        // when
        ChatbotQueryResponse result = client.query(request(), "trace-id");

        // then
        assertThat(result.status()).isEqualTo(ChatbotResponseStatus.COMPLETED);
        assertThat(result.answer()).isEqualTo("통합 응답");
        assertThat(capturedRequest.get().internalApiKey()).isEqualTo("internal-key");
        assertThat(capturedRequest.get().traceId()).isEqualTo("trace-id");
        assertThat(capturedRequest.get().body()).contains("\"timeoutMs\":30000");
    }

    @ParameterizedTest(name = "Chatbot {0} 응답은 {1}로 변환된다")
    @MethodSource("errorStatuses")
    @DisplayName("Chatbot 오류 상태를 Backend 도메인 오류로 변환한다")
    void query_whenHttpCallReturnsError_mapsStatus(int status, ChatErrorCode expectedErrorCode) {
        // given
        response.set(new Response(status, "{\"code\":\"UPSTREAM_ERROR\"}", 0));
        RestChatbotClient client = createClient(1000);

        // when & then
        assertThatThrownBy(() -> client.query(request(), "trace-id"))
                .isInstanceOf(ChatException.class)
                .satisfies(error -> assertThat(((ChatException) error).getErrorCode())
                        .isEqualTo(expectedErrorCode));
    }

    @Test
    @DisplayName("Chatbot 응답이 HTTP 읽기 제한 시간을 초과하면 504 오류로 변환한다")
    void query_whenHttpReadTimesOut_mapsToGatewayTimeout() {
        // given
        response.set(new Response(200, RESPONSE_BODY, 300));
        RestChatbotClient client = createClient(100);

        // when & then
        assertThatThrownBy(() -> client.query(request(), "trace-id"))
                .isInstanceOf(ChatException.class)
                .satisfies(error -> assertThat(((ChatException) error).getErrorCode())
                        .isEqualTo(ChatErrorCode.CHATBOT_TIMEOUT));
    }

    private RestChatbotClient createClient(long timeoutMs) {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new RestChatbotClient(
                new ChatbotClientConfig().chatbotRestClient(baseUrl, timeoutMs),
                "internal-key"
        );
    }

    private ChatbotQueryRequest request() {
        return new ChatbotQueryRequest(
                "request-id", "session-id", 1L, "질문", List.of(), 30000L, "ko");
    }

    private void handleQuery(HttpExchange exchange) throws IOException {
        capturedRequest.set(new CapturedRequest(
                exchange.getRequestHeaders().getFirst("X-Internal-Api-Key"),
                exchange.getRequestHeaders().getFirst("X-Trace-Id"),
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
        ));
        Response configuredResponse = response.get();
        if (configuredResponse.delayMs() > 0) {
            try {
                Thread.sleep(configuredResponse.delayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] body = configuredResponse.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        try {
            exchange.sendResponseHeaders(configuredResponse.status(), body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    private static Stream<Arguments> errorStatuses() {
        return Stream.of(
                Arguments.of(400, ChatErrorCode.CHATBOT_BAD_REQUEST),
                Arguments.of(401, ChatErrorCode.CHATBOT_UNAUTHORIZED),
                Arguments.of(409, ChatErrorCode.CHATBOT_REQUEST_CONFLICT),
                Arguments.of(500, ChatErrorCode.CHATBOT_INTERNAL_ERROR),
                Arguments.of(503, ChatErrorCode.CHATBOT_UNAVAILABLE),
                Arguments.of(504, ChatErrorCode.CHATBOT_TIMEOUT)
        );
    }

    private record Response(int status, String body, long delayMs) {
    }

    private record CapturedRequest(String internalApiKey, String traceId, String body) {
    }
}
