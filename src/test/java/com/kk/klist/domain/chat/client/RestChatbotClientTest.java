package com.kk.klist.domain.chat.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kk.klist.domain.chat.domain.exception.ChatErrorCode;
import com.kk.klist.domain.chat.domain.exception.ChatException;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotContextMessage;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotAudioQueryRequest;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotAudioQueryResponse;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotQueryRequest;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotQueryResponse;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotResponseStatus;
import com.kk.klist.domain.chat.domain.ChatMessageRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestClient;

class RestChatbotClientTest {

    private MockRestServiceServer server;
    private RestChatbotClient chatbotClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://chatbot");
        server = MockRestServiceServer.bindTo(builder).build();
        chatbotClient = new RestChatbotClient(builder.build(), "internal-key");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ko", "en"})
    @DisplayName("Chatbot 내부 API에 인증 키와 trace ID 및 질문 요청을 전달한다")
    void query_whenChatbotResponds_returnsResponse(String language) {
        // given
        ChatbotQueryRequest request = new ChatbotQueryRequest(
                "request-id",
                "session-id",
                1L,
                "현재 질문",
                List.of(new ChatbotContextMessage(ChatMessageRole.USER, "이전 질문")),
                5000L, language
        );
        server.expect(once(), requestTo("http://chatbot/internal/chat/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Api-Key", "internal-key"))
                .andExpect(header("X-Trace-Id", "trace-id"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"language\":\"" + language + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"requestId\":\"request-id\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"timeoutMs\":5000")))
                .andRespond(withSuccess(
                        "{\"status\":\"COMPLETED\",\"answer\":\"완료된 답변\"," +
                                "\"suggestions\":[\"다른 관광지도 알려줘\"]}",
                        MediaType.APPLICATION_JSON
                ));

        // when
        ChatbotQueryResponse response = chatbotClient.query(request, "trace-id");

        // then
        assertThat(response.status()).isEqualTo(ChatbotResponseStatus.COMPLETED);
        assertThat(response.answer()).isEqualTo("완료된 답변");
        assertThat(response.suggestions()).containsExactly("다른 관광지도 알려줘");
        server.verify();
    }

    @Test
    @DisplayName("Chatbot 내부 API가 500을 반환하면 ChatbotInternalError 예외가 발생한다")
    void query_whenChatbotReturns500_throwsChatbotInternalError() {
        // given
        ChatbotQueryRequest request = new ChatbotQueryRequest(
                "request-id", "session-id", 1L, "질문", List.of(), 5000L, "ko");
        server.expect(once(), requestTo("http://chatbot/internal/chat/query"))
                .andRespond(withServerError());

        // when & then
        assertThatThrownBy(() -> chatbotClient.query(request, "trace-id"))
                .isInstanceOf(ChatException.class)
                .satisfies(error -> assertThat(((ChatException) error).getErrorCode())
                        .isEqualTo(ChatErrorCode.CHATBOT_INTERNAL_ERROR));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ko", "en"})
    @DisplayName("Chatbot 음성 API에 요청 정보와 음성 파일을 multipart로 전달한다")
    void queryAudio_whenChatbotResponds_returnsTranscription(String language) {
        // given
        ChatbotAudioQueryRequest request = new ChatbotAudioQueryRequest(
                "request-id",
                "session-id",
                1L,
                List.of(new ChatbotContextMessage(ChatMessageRole.USER, "이전 질문")),
                5000L, language
        );
        MockMultipartFile audio = new MockMultipartFile(
                "audio", "question.webm", "audio/webm", "audio-data".getBytes());
        server.expect(once(), requestTo("http://chatbot/internal/chat/query/audio"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Api-Key", "internal-key"))
                .andExpect(header("X-Trace-Id", "trace-id"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"language\":\"" + language + "\"")))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.containsString("multipart/form-data")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"request\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"requestId\":\"request-id\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("filename=\"question.webm\"")))
                .andRespond(withSuccess(
                        "{\"status\":\"COMPLETED\",\"transcription\":\"서울 관광지를 추천해줘\"," +
                                "\"answer\":\"경복궁을 추천합니다.\"," +
                                "\"suggestions\":[\"주변 맛집도 알려줘\"]}",
                        MediaType.APPLICATION_JSON
                ));

        // when
        ChatbotAudioQueryResponse response = chatbotClient.queryAudio(request, audio, "trace-id");

        // then
        assertThat(response.status()).isEqualTo(ChatbotResponseStatus.COMPLETED);
        assertThat(response.transcription()).isEqualTo("서울 관광지를 추천해줘");
        assertThat(response.answer()).isEqualTo("경복궁을 추천합니다.");
        server.verify();
    }
}
