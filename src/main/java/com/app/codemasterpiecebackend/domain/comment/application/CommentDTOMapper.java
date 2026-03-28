package com.app.codemasterpiecebackend.domain.comment.application;

import com.app.codemasterpiecebackend.domain.comment.dto.CommentDTO;
import com.app.codemasterpiecebackend.domain.comment.entity.Comment;
import com.app.codemasterpiecebackend.domain.shared.security.ActorProvider;
import com.app.codemasterpiecebackend.global.util.MarkdownUtil;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * 엔티티와 DTO 간의 변환을 담당하는 유틸리티 클래스입니다.
 * 상태를 가지지 않으므로 정적(static) 메서드로만 구성됩니다.
 */
final class CommentDTOMapper {

    // 인스턴스화 원천 차단
    private CommentDTOMapper() {}

    /**
     * 엔티티 기반 기본 매핑.
     * - 추가 쿼리 유발 없음
     * - parentId는 프록시 초기화 없이 id만 참조
     * - reaction/myReaction/hasChildren/children은 기본값
     */
    static CommentDTO toDtoBasic(Comment c) {
        Objects.requireNonNull(c, "comment must not be null");

        final String parentId = safeId(c.getParent());
        final boolean anon = c.getActorProvider() == ActorProvider.ANON;

        return new CommentDTO(
                c.getId(),
                parentId,
                c.getActorId(),
                c.getActorSnapshot() != null ? c.getActorSnapshot().getImageUrl() : null,
                c.getActorSnapshot() != null ? c.getActorSnapshot().getDisplayName() : null,
                MarkdownUtil.parseCommentToHtml(c.getContent()),
                0,
                null,
                c.getDepth(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.isHidden(),
                c.isDeleted(),
                anon,
                false,
                List.of()
        );
    }

    /**
     * MyBatis가 만든 평면 DTO를 트리화 단계에서 보강.
     * - children/hasChildren만 교체
     */
    static CommentDTO mergeFlatDto(CommentDTO base, List<CommentDTO> children) {
        boolean has = children != null && !children.isEmpty();
        return base.withChildren(children != null ? children : List.of())
                .withHasChildren(has);
    }

    @Nullable
    private static String safeId(@Nullable Comment ref) {
        return ref == null ? null : ref.getId();
    }
}