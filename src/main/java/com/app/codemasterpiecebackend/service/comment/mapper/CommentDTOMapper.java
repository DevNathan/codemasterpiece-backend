package com.app.codemasterpiecebackend.service.comment.mapper;

import com.app.codemasterpiecebackend.domain.dto.comment.CommentDTO;
import com.app.codemasterpiecebackend.domain.entity.comment.Comment;
import com.app.codemasterpiecebackend.domain.entity.comment.ReactionValue;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class CommentDTOMapper {

    /**
     * 엔티티 기반 기본 매핑.
     * - 추가 쿼리 유발 없음
     * - parentId는 프록시 초기화 없이 id만 참조
     * - reaction/myReaction/hasChildren/children은 기본값
     */
    public CommentDTO toDtoBasic(Comment c) {
        Objects.requireNonNull(c, "comment must not be null");

        final String parentId = safeId(c.getParent());
        final boolean anon = c.getActorProvider() == ActorProvider.ANON;

        return new CommentDTO(
                c.getId(),
                parentId,
                c.getActorId(),
                c.getActorSnapshot() != null ? c.getActorSnapshot().getImageUrl() : null,
                c.getActorSnapshot() != null ? c.getActorSnapshot().getDisplayName() : null,
                c.getContent(),
                0,          // 합산 점수는 쿼리에서 계산
                null,       // 내 리액션도 쿼리에서
                c.getDepth(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.isHidden(),
                c.isDeleted(),
                anon,
                false,      // 트리 구성 시 세팅
                List.of()
        );
    }

    /**
     * MyBatis가 만든 평면 DTO를 트리화 단계에서 보강.
     * - children/hasChildren만 교체
     */
    public CommentDTO mergeFlatDto(CommentDTO base, List<CommentDTO> children) {
        boolean has = children != null && !children.isEmpty();
        return base.withChildren(children != null ? children : List.of())
                .withHasChildren(has);
    }

    // ===== Helpers =====

    /** Hibernate 프록시 초기화 없이 ID만 안전 추출 */
    @Nullable
    private String safeId(@Nullable Comment ref) {
        return ref == null ? null : ref.getId();
    }
}
