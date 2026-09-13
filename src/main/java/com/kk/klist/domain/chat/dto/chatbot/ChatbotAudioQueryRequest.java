package com.kk.klist.domain.chat.dto.chatbot;

import java.util.List;

public record ChatbotAudioQueryRequest(
        String requestId,
        String sessionId,
        Long userId,
        List<ChatbotContextMessage> context,
        long timeoutMs,
        String language
) {
}
