package com.project.distributed_lovable.intelligence_service.entity;

import lombok.*;

import java.io.Serializable;
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Getter
@Setter
public class ChatSessionId  implements Serializable {
    Long projectId;
    Long userId;
}
