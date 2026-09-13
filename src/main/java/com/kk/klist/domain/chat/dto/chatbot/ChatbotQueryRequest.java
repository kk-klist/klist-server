package com.kk.klist.domain.chat.dto.chatbot;

import java.util.List;

public record ChatbotQueryRequest(
        String requestId,
        String sessionId,
        Long userId,
        String message,
        List<ChatbotContextMessage> context,
        long timeoutMs,
        String language
) {
}
