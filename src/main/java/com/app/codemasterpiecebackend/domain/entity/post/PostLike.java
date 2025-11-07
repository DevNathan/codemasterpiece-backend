package com.app.codemasterpiecebackend.domain.entity.post;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_post_like",
        indexes = {
                @Index(name = "idx_like_post_created", columnList = "post_id, created_at"),
                @Index(name = "idx_like_actor", columnList = "actor_provider, actor_id")
        },
        uniqueConstraints = {
                // 한 행위자(actor)당 같은 포스트에 한 번만 좋아요
                @UniqueConstraint(
                        name = "uq_like_post_actor",
                        columnNames = {"post_id", "actor_provider", "actor_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@DynamicUpdate
public class PostLike {
    @Id
    @PrefixedUlidId("LK")
    @Column(name = "like_id", nullable = false, updatable = false,  length = 29)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            referencedColumnName = "post_id",
            foreignKey = @ForeignKey(name = "fk_like_post")
    )
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_provider", nullable = false, length = 16)
    private ActorProvider actorProvider;

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
