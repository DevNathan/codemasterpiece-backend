package com.app.codemasterpiecebackend.service.comment;

import com.app.codemasterpiecebackend.domain.dto.comment.CommentDTO;
import com.app.codemasterpiecebackend.domain.entity.comment.ReactionValue;
import com.app.codemasterpiecebackend.service.comment.cmd.*;
import org.springframework.data.domain.Page;

/**
 * Comment 도메인의 애플리케이션 서비스 인터페이스.
 * <p>
 * 설계 원칙:
 * <ul>
 *   <li>엔티티 변경(write)은 트랜잭션 내에서 수행</li>
 *   <li>조회(read)는 MyBatis 쿼리로 화면 필드를 모두 계산해 DTO로 직출</li>
 *   <li>매퍼는 DB에 접근하지 않음: 값 복사만</li>
 * </ul>
 */
public interface CommentService {

    // ===== C(reate) =====

    /**
     * 댓글을 생성한다.
     *
     * @param cmd 생성 명령 (postId, parentId, content, actor 정보 등)
     * @return 생성 직후의 화면 응답용 DTO (집계값은 0/NULL)
     */
    CommentDTO create(CommentCreateCmd cmd);

    // ===== R(ead) =====

    /**
     * 특정 게시글의 댓글 페이지를 트리 형태로 반환한다.
     * <p>MyBatis에서 평면 DTO를 조회한 뒤, 메모리에서 트리화한다.</p>
     *
     * @param cmd 조회 명령 (postId, elevated, actorId, pageable)
     * @return 트리화된 댓글 페이지 (root 기준 페이징)
     */
    Page<CommentDTO> getPageByPostId(CommentViewCmd cmd);

    // ===== U(pdate) =====

    /**
     * 댓글 내용을 수정한다.
     * <p>권한 검증 후 내용만 갱신한다.</p>
     *
     * @param cmd 수정 명령 (commentId, content, 인증정보)
     * @return 수정 결과의 간단 DTO(집계값은 포함하지 않음)
     */
    CommentDTO update(CommentUpdateCmd cmd);

    // ===== D(elete) =====

    /**
     * 댓글을 삭제한다.
     * <p>활성 자식이 있으면 soft-delete, 없으면 hard-delete.
     * 하드 삭제 후 상위 soft 연쇄 정리.</p>
     *
     * @param cmd 삭제 명령 (commentId, 인증정보)
     */
    void delete(CommentDeleteCmd cmd);

    // ===== Extra (Domain actions) =====

    /**
     * 댓글의 숨김 여부를 토글한다.
     *
     * @param cmd 토글 명령 (commentId, setHidden)
     * @return 실제 DB 반영 결과가 요청과 일치하면 true
     */
    boolean toggleHide(CommentLikeCmd cmd);

    /**
     * 댓글 리액션을 토글/설정하고 현재 나의 리액션을 반환한다.
     *
     * @param cmd 리액션 명령 (commentId, actorProvider, actorId, value)
     * @return 나의 리액션(enum) 또는 null
     */
    ReactionValue react(CommentReactCmd cmd);
}
