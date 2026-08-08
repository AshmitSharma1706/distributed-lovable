package com.project.distributed_lovable.intelligence_service.dto.chat;

import com.project.distributed_lovable.common_lib.enums.MessageRole;
import com.project.distributed_lovable.intelligence_service.entity.ChatSession;

import java.time.Instant;
import java.util.List;

public record ChatResponse(
        Long id,
        ChatSession chatSession,
        List<ChatEventResponse>events,
        String content,
        MessageRole role,
        Integer tokensUsed,
        Instant createdAt
) {

}
