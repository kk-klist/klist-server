package com.kk.klist.domain.chat.service;

import com.kk.klist.domain.chat.domain.ChatContextMessage;
import com.kk.klist.domain.chat.client.ChatbotClient;
import com.kk.klist.domain.chat.domain.exception.ChatErrorCode;
import com.kk.klist.domain.chat.domain.exception.ChatException;
import com.kk.klist.domain.chat.dto.response.ChatSessionCreateResponse;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotAudioQueryRequest;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotAudioQueryResponse;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotContextMessage;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotQueryRequest;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotQueryResponse;
import com.kk.klist.domain.chat.dto.chatbot.ChatbotResponseStatus;
import com.kk.klist.domain.chat.dto.request.ChatQueryRequest;
import com.kk.klist.domain.chat.dto.response.ChatAudioQueryResponse;
import com.kk.klist.domain.chat.dto.response.ChatQueryResponse;
import com.kk.klist.domain.chat.repository.ChatSessionRepository;
import com.kk.klist.global.util.TimeProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ChatService {

    static final Duration SESSION_TTL = Duration.ofMinutes(3);
    static final int CONTEXT_LIMIT = 10;
    static final long CHATBOT_TIMEOUT_MS = 30000L;

    private final ChatSessionRepository chatSessionRepository;
    private final ChatSessionIdGenerator sessionIdGenerator;
    private final TimeProvider timeProvider;
    private final ChatbotClient chatbotClient;

    public ChatSessionCreateResponse createSession(Long userId) {
        String sessionId = sessionIdGenerator.generate();
        LocalDateTime expiresAt = timeProvider.now().plus(SESSION_TTL);

        chatSessionRepository.save(sessionId, userId, SESSION_TTL);

        return ChatSessionCreateResponse.of(sessionId, expiresAt);
    }

    public void validateSessionOwnership(Long userId, String sessionId) {
        Long ownerId = chatSessionRepository.findOwner(sessionId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.SESSION_NOT_FOUND));
        if (!ownerId.equals(userId)) {
            throw new ChatException(ChatErrorCode.SESSION_ACCESS_DENIED);
        }
    }

    public List<ChatContextMessage> getRecentContext(Long userId, String sessionId) {
        validateSessionOwnership(userId, sessionId);
        return chatSessionRepository.findRecentContext(sessionId, CONTEXT_LIMIT);
    }

    public void saveCompletedExchange(
            Long userId,
            String sessionId,
            String userMessage,
            String assistantMessage
    ) {
        validateSessionOwnership(userId, sessionId);
        chatSessionRepository.saveCompletedExchange(
                sessionId,
                ChatContextMessage.user(userMessage),
                ChatContextMessage.assistant(assistantMessage),
                SESSION_TTL,
                CONTEXT_LIMIT
        );
    }

    public ChatQueryResponse query(Long userId, ChatQueryRequest request) {
        validateSessionOwnership(userId, request.sessionId());

        String requestId = sessionIdGenerator.generate();
        String traceId = sessionIdGenerator.generate();
        List<ChatbotContextMessage> context = chatSessionRepository
                .findRecentContext(request.sessionId(), CONTEXT_LIMIT)
                .stream()
                .map(ChatbotContextMessage::from)
                .toList();
        ChatbotQueryRequest chatbotRequest = new ChatbotQueryRequest(
                requestId,
                request.sessionId(),
                userId,
                request.message(),
                context,
                CHATBOT_TIMEOUT_MS,
                request.language() == null ? "ko" : request.language()
        );

        ChatbotQueryResponse chatbotResponse = chatbotClient.query(chatbotRequest, traceId);
        validateChatbotResponse(chatbotResponse);
        if (chatbotResponse.status() == ChatbotResponseStatus.COMPLETED) {
            chatSessionRepository.saveCompletedExchange(
                    request.sessionId(),
                    ChatContextMessage.user(request.message()),
                    ChatContextMessage.assistant(chatbotResponse.answer()),
                    SESSION_TTL,
                    CONTEXT_LIMIT
            );
        }

        return ChatQueryResponse.from(requestId, request.sessionId(), traceId, chatbotResponse);
    }

    public ChatAudioQueryResponse queryAudio(Long userId, String sessionId, MultipartFile audio, String language) {
        if (audio == null || audio.isEmpty()) {
            throw new ChatException(ChatErrorCode.AUDIO_FILE_EMPTY);
        }
        validateSessionOwnership(userId, sessionId);

        String requestId = sessionIdGenerator.generate();
        String traceId = sessionIdGenerator.generate();
        List<ChatbotContextMessage> context = chatSessionRepository
                .findRecentContext(sessionId, CONTEXT_LIMIT)
                .stream()
                .map(ChatbotContextMessage::from)
                .toList();
        ChatbotAudioQueryRequest chatbotRequest = new ChatbotAudioQueryRequest(
                requestId,
                sessionId,
                userId,
                context,
                CHATBOT_TIMEOUT_MS,
                language == null ? "ko" : language
        );

        ChatbotAudioQueryResponse chatbotResponse = chatbotClient
                .queryAudio(chatbotRequest, audio, traceId);
        validateAudioChatbotResponse(chatbotResponse);
        if (chatbotResponse.status() == ChatbotResponseStatus.COMPLETED) {
            chatSessionRepository.saveCompletedExchange(
                    sessionId,
                    ChatContextMessage.user(chatbotResponse.transcription()),
                    ChatContextMessage.assistant(chatbotResponse.answer()),
                    SESSION_TTL,
                    CONTEXT_LIMIT
            );
        }

        return ChatAudioQueryResponse.from(requestId, sessionId, traceId, chatbotResponse);
    }

    private void validateChatbotResponse(ChatbotQueryResponse response) {
        if (response == null
                || response.status() == null
                || response.answer() == null
                || response.answer().isBlank()
                || response.suggestions() == null
                || response.suggestions().stream().anyMatch(suggestion -> suggestion == null || suggestion.isBlank())) {
            throw new ChatException(ChatErrorCode.CHATBOT_INVALID_RESPONSE);
        }
    }

    private void validateAudioChatbotResponse(ChatbotAudioQueryResponse response) {
        if (response == null
                || response.transcription() == null
                || response.transcription().isBlank()) {
            throw new ChatException(ChatErrorCode.STT_INVALID_RESPONSE);
        }
        if (response.status() == null
                || response.answer() == null
                || response.answer().isBlank()
                || response.suggestions() == null
                || response.suggestions().stream()
                        .anyMatch(suggestion -> suggestion == null || suggestion.isBlank())) {
            throw new ChatException(ChatErrorCode.CHATBOT_INVALID_RESPONSE);
        }
    }
}
