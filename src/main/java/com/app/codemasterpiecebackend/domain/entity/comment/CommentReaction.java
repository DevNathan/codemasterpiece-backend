package com.app.codemasterpiecebackend.domain.entity.comment;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import com.app.codemasterpiecebackend.domain.entity.base.BaseTimeEntity;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

@Getter
@ToString(exclude = {"comment"})
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
@Builder
@Entity
@Table(
        name = "tbl_comment_reaction",
        uniqueConstraints = {
                // 한 댓글 + 한 배우 조합은 하나의 반응만 허용
                @UniqueConstraint(
                        name = "uk_comment_reaction_actor",
                        columnNames = {"comment_id", "actor_provider", "actor_id"}
                )
        }
)
public class CommentReaction extends BaseTimeEntity {

    @Id
    @PrefixedUlidId("CR")
    @Column(name = "reaction_id", nullable = false, length = 29)
    private String id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "comment_id",
            referencedColumnName = "comment_id",
            foreignKey = @ForeignKey(name = "fk_comment_reaction_comment"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_provider", nullable = false, length = 16)
    private ActorProvider actorProvider; // GITHUB / ANON

    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId; // 깃허브 유저ID or 익명 UUID

    @Enumerated(EnumType.STRING)
    @Column(name = "value", nullable = false, length = 10)
    private ReactionValue value; // UPVOTE / DOWNVOTE

    // === 도메인 로직 ===
    public void switchTo(ReactionValue newValue) {
        if (newValue == null) throw new IllegalArgumentException("newValue is null");
        this.value = newValue;
    }
}
