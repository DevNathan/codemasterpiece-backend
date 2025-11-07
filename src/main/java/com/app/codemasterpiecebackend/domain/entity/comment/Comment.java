package com.app.codemasterpiecebackend.domain.entity.comment;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import com.app.codemasterpiecebackend.domain.entity.base.BaseTimeEntity;
import com.app.codemasterpiecebackend.domain.entity.post.Post;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Getter
@ToString(exclude = {"post", "parent", "children"})
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Entity
@Table(
        name = "tbl_comment",
        indexes = {
                @Index(name = "idx_comment_post_created", columnList = "post_id, created_at"),
                @Index(name = "idx_comment_parent_created", columnList = "parent_id, created_at")
        }
)
@Builder
public class Comment extends BaseTimeEntity {

    @Id
    @PrefixedUlidId("CO")
    @Column(name = "comment_id", nullable = false, length = 29)
    private String id;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "post_id",
            referencedColumnName = "post_id",
            foreignKey = @ForeignKey(name = "fk_comment_post"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    // 배우(Actor) 식별자
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_provider", nullable = false, length = 16)
    private ActorProvider actorProvider;

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId; // 등록 유저면 userId, 게스트면 ULID/세션-토큰 등

    // 화면용 스냅샷(닉네임/이미지)
    @Embedded
    private ActorSnapshot actorSnapshot;

    // 게스트 전용 4자리 PIN 해시 (비익명은 null)
    @Embedded
    private GuestAuth guestAuth;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id",
            foreignKey = @ForeignKey(name = "fk_comment_parent"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @Builder.Default
    private List<Comment> children = new ArrayList<>();

    @Column(name = "depth", nullable = false)
    @Builder.Default
    private int depth = 0;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private boolean hidden = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    public void updateContent(String content) {
        this.content = content;
    }
}

