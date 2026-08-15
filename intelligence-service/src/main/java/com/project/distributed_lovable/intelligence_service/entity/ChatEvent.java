package com.project.distributed_lovable.intelligence_service.entity;

import com.project.distributed_lovable.common_lib.enums.ChatEventStatus;
import com.project.distributed_lovable.common_lib.enums.ChatEventType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "chat_events")
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChatEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    ChatMessage chatMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ChatEventType type;

    @Column(nullable = false)
    Integer sequenceOrder;

    @Column(columnDefinition = "text")
    String content;

    @Column(columnDefinition = "text")
    String metadata;

    String sagaId;

    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
    ChatEventStatus status;

    String filePath;
}
