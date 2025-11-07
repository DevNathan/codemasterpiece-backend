package com.app.codemasterpiecebackend.domain.entity.guestbook;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import com.app.codemasterpiecebackend.domain.entity.base.BaseTimeEntity;
import com.app.codemasterpiecebackend.domain.entity.comment.ActorSnapshot;
import com.app.codemasterpiecebackend.domain.entity.comment.GuestAuth;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Builder
@Entity
@Table(
        name = "tbl_guestbook",
        indexes = {
                // 커서(키셋) 페이지네이션용 복합 인덱스: created_at DESC, id DESC
                @Index(name = "idx_guestbook_created_id_desc", columnList = "created_at DESC, guestbook_id DESC")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@DynamicUpdate
public class GuestbookEntry extends BaseTimeEntity {

    @Id
    @PrefixedUlidId("GB")
    @Column(name = "guestbook_id", columnDefinition = "char(29)")
    private String id;

    @Column(length = 2000, nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_provider", nullable = false, length = 16)
    private ActorProvider actorProvider;

    /**
     * 등록 유저면 userId, 게스트면 세션/ULID 등 외부 식별자.
     * 비즈니스 규칙상 NOT NULL.
     */
    @Column(name = "actor_id", nullable = false, length = 100)
    private String actorId;

    /**
     * 렌더/표시에 필요한 스냅샷(닉네임/이미지 등). GITHUB/ANON 모두 보유 권장.
     */
    @Embedded
    private ActorSnapshot actorSnapshot;

    /**
     * 게스트 보안정보(예: PIN 해시). GITHUB일 땐 null이어야 함.
     * 민감정보이므로 toString 제외.
     */
    @Embedded
    private GuestAuth guestAuth;

    public void updateContent(String content) {
        this.content = content;
    }
}
