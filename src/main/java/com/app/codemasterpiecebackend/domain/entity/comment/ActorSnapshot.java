package com.app.codemasterpiecebackend.domain.entity.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class ActorSnapshot {
    @Column(name = "actor_display_name", nullable = false, length = 60)
    private String displayName;

    @Column(name = "actor_image_url", length = 512)
    private String imageUrl; // null 가능 (기본 아바타 처리)
}