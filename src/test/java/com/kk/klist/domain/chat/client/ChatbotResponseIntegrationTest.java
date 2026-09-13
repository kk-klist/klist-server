package com.kk.klist.domain.chat.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.kk.klist.domain.chat.config.ChatbotClientConfig;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ChatbotResponseIntegrationTest {

    private HttpServer server;
    private final AtomicReference<ChatbotResponseStatus> responseStatus = new AtomicReference<>();

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

    @ParameterizedTest
    @EnumSource(ChatbotResponseStatus.class)
    @DisplayName("실제 Chatbot HTTP 응답의 모든 정상 상태와 사용자용 응답을 역직렬화한다")
    void query_whenChatbotReturnsNormalStatus_deserializesCompleteResponse(
            ChatbotResponseStatus status
    ) {
        // given
        responseStatus.set(status);
        RestChatbotClient client = createClient();

        // when
        ChatbotQueryResponse response = client.query(new ChatbotQueryRequest(
                "request-id",
                "session-id",
                1L,
                "질문",
                List.of(),
                5000L, "ko"
        ), "trace-id");

        // then
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.answer()).isEqualTo("사용자용 답변");
        assertThat(response.suggestions()).containsExactly("후속 질문 1", "후속 질문 2");
    }

    private RestChatbotClient createClient() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new RestChatbotClient(
                new ChatbotClientConfig().chatbotRestClient(baseUrl, 1000L),
                "internal-key"
        );
    }

    private void handleQuery(HttpExchange exchange) throws IOException {
        String body = """
                {"status":"%s","answer":"사용자용 답변",\
                "suggestions":["후속 질문 1","후속 질문 2"]}
                """.formatted(responseStatus.get().name());
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBody.length);
        try {
            exchange.getResponseBody().write(responseBody);
        } finally {
            exchange.close();
        }
    }
}
