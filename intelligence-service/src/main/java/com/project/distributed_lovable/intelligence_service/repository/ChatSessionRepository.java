package com.project.distributed_lovable.intelligence_service.repository;

import com.project.distributed_lovable.intelligence_service.entity.ChatSession;
import com.project.distributed_lovable.intelligence_service.entity.ChatSessionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, ChatSessionId> {

}
