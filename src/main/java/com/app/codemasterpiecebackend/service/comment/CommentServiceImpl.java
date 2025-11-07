package com.app.codemasterpiecebackend.service.comment;

import com.app.codemasterpiecebackend.domain.dto.comment.CommentDTO;
import com.app.codemasterpiecebackend.domain.entity.comment.ActorSnapshot;
import com.app.codemasterpiecebackend.domain.entity.comment.Comment;
import com.app.codemasterpiecebackend.domain.entity.comment.GuestAuth;
import com.app.codemasterpiecebackend.domain.entity.comment.ReactionValue;
import com.app.codemasterpiecebackend.domain.entity.post.Post;
import com.app.codemasterpiecebackend.domain.repository.CommentReactionRepository;
import com.app.codemasterpiecebackend.domain.repository.CommentRepository;
import com.app.codemasterpiecebackend.domain.repository.PostRepository;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import com.app.codemasterpiecebackend.mapper.CommentMapper;
import com.app.codemasterpiecebackend.service.comment.cmd.*;
import com.app.codemasterpiecebackend.service.comment.mapper.CommentDTOMapper;
import com.app.codemasterpiecebackend.support.exception.AppException;
import com.app.codemasterpiecebackend.support.exception.FieldValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Comment 애플리케이션 서비스 구현.
 * <p>
 * 원칙:
 * <ul>
 *   <li>조회는 MyBatis로 모든 화면 필드(리액션 합/내 리액션/자식여부)까지 계산</li>
 *   <li>서비스는 평면 리스트를 트리로 묶는 최소 로직만 수행</li>
 *   <li>엔티티→DTO 매퍼는 DB에 접근하지 않으며, 연관은 id만 읽는다(지연 초기화 금지)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final CommentReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommentDTOMapper dtoMapper;

    // ===== C(reate) =====

    /**
     * {@inheritDoc}
     */
    @Override
    public CommentDTO create(CommentCreateCmd cmd) {
        final Post postRef = postRepository.getReferenceById(cmd.postId());

        int depth = 0;
        Comment parentRef = null;
        if (cmd.parentId() != null) {
            // 부모 depth만 끊어와 depth+1 계산 (1쿼리)
            var briefOpt = commentRepository.findParentBrief(cmd.parentId());
            int parentDepth = briefOpt.map(CommentRepository.ParentBrief::getDepth).orElse(0);
            depth = parentDepth + 1;

            // 프록시 참조(추가 쿼리 없음, id 접근만 수행)
            parentRef = commentRepository.getReferenceById(cmd.parentId());
        }

        Comment.CommentBuilder builder = Comment.builder()
                .post(postRef)
                .content(cmd.content())
                .parent(parentRef)
                .depth(depth);

        if (cmd.actor().provider() == ActorProvider.ANON) {
            var g = cmd.guest();
            builder.actorProvider(ActorProvider.ANON)
                    .actorId(cmd.actor().actorId())
                    .actorSnapshot(
                            ActorSnapshot.builder()
                                    .displayName(cmd.displayName())
                                    .imageUrl(g.imageUrl())
                                    .build()
                    )
                    .guestAuth(
                            GuestAuth.builder()
                                    .pinHash(passwordEncoder.encode(g.pin()))
                                    .build()
                    );
        } else { // GITHUB
            builder.actorProvider(ActorProvider.GITHUB)
                    .actorId(cmd.actor().actorId())
                    .actorSnapshot(
                            ActorSnapshot.builder()
                                    .displayName(cmd.displayName())
                                    .imageUrl("https://avatars.githubusercontent.com/u/" + cmd.actor().actorId() + "?v=4")
                                    .build()
                    );
        }

        Comment saved = commentRepository.save(builder.build());

        // 생성 응답은 재조회 없이 엔티티->DTO (집계값은 기본값)
        return dtoMapper.toDtoBasic(saved);
    }

    // ===== R(ead) =====

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentDTO> getPageByPostId(CommentViewCmd cmd) {
        int limit = cmd.pageable().getPageSize();
        long offset = cmd.pageable().getOffset();

        // 화면 필드까지 완성된 평면 DTO 조회 (MyBatis)
        List<CommentDTO> flat = commentMapper.findCommentsByPostId(
                cmd.postId(),
                cmd.elevated(),
                cmd.actorId(),
                limit,
                offset
        );

        // 트리 구성: children/hasChildren만 보강
        List<CommentDTO> roots = toTree(flat);

        long totalRoots = commentMapper.countCommentsByPostId(cmd.postId());
        return new PageImpl<>(roots, cmd.pageable(), totalRoots);
    }

    // ===== U(pdate) =====

    /**
     * {@inheritDoc}
     */
    @Override
    public CommentDTO update(CommentUpdateCmd cmd) {
        Comment comment = commentRepository.findById(cmd.commentId()).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "error.comment.not_found")
        );

        ensureModifiable(comment, cmd.userId(), cmd.password(), cmd.elevated());
        comment.updateContent(cmd.content());

        // 즉시 재조회 없이 엔티티 기반 최소 DTO 반환 (집계값 없음)
        return dtoMapper.toDtoBasic(comment);
    }

    // ===== D(elete) =====

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(CommentDeleteCmd cmd) {
        Comment target = commentRepository.findWithParent(cmd.commentId()).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "error.comment.not_found")
        );

        ensureModifiable(target, cmd.userId(), cmd.password(), cmd.elevated());

        boolean hasActiveChild = commentRepository.existsActiveChild(target.getId());
        if (hasActiveChild) {
            // 활성 자식 존재 → soft-delete
            commentRepository.softDelete(target.getId());
            return;
        }

        // 활성 자식 없음 → hard-delete
        String parentId = commentRepository.findParentId(target.getId()).orElse(null);
        commentRepository.deleteById(target.getId());

        // 상향 정리: soft 상태의 상위가 더 이상 활성 자식 없으면 연쇄 하드 삭제
        while (parentId != null) {
            String currentId = parentId;

            Comment parent = commentRepository.findWithParent(currentId).orElse(null);
            if (parent == null) break;
            if (!parent.isDeleted()) break;
            if (commentRepository.existsActiveChild(currentId)) break;

            parentId = commentRepository.findParentId(currentId).orElse(null);
            commentRepository.deleteById(currentId);
        }
    }

    // ===== Extra (Domain actions) =====

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean toggleHide(CommentLikeCmd cmd) {
        int updated = commentRepository.updateHiddenById(cmd.commentId(), cmd.setHidden());
        return (updated > 0) == cmd.setHidden();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable ReactionValue react(CommentReactCmd cmd) {
        String result = reactionRepository.reactAndGetMyReaction(
                cmd.commentId(),
                cmd.actorProvider().name(),
                cmd.actorId(),
                cmd.value() == null ? null : cmd.value().name()
        );

        if (result == null) return null;
        try {
            return ReactionValue.valueOf(result);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ===== Private helpers =====

    /**
     * 수정/삭제 권한 검증.
     * <ul>
     *   <li>elevated=true → 무조건 허용</li>
     *   <li>GITHUB → 소유자만 허용</li>
     *   <li>ANON → PIN 일치 시 허용</li>
     * </ul>
     */
    private void ensureModifiable(Comment comment, String userId, String password, boolean elevated) {
        final ActorProvider provider = comment.getActorProvider();

        if (elevated) return;

        if (provider == ActorProvider.GITHUB) {
            boolean isOwner = comment.getActorId() != null && comment.getActorId().equals(userId);
            if (isOwner) return;
            throw new AppException(HttpStatus.FORBIDDEN, "error.forbidden");
        }

        if (provider == ActorProvider.ANON) {
            boolean pinOk = comment.getGuestAuth() != null &&
                    password != null &&
                    passwordEncoder.matches(password, comment.getGuestAuth().getPinHash());
            if (pinOk) return;

            // 필드 단위 검증 에러로 처리
            throw new FieldValidationException(Map.of("guestPassword", "error.comment.invalid_passwd"));
        }

        throw new AppException(HttpStatus.FORBIDDEN, "error.forbidden");
    }

    /**
     * 평면 댓글 리스트를 트리로 구성한다.
     * <p>쿼리 수를 늘리지 않기 위해 메모리에서만 처리.</p>
     */
    private List<CommentDTO> toTree(List<CommentDTO> flat) {
        Map<String, List<CommentDTO>> childrenBucket = new LinkedHashMap<>();
        List<CommentDTO> roots = new ArrayList<>();

        for (CommentDTO c : flat) {
            String pid = c.getParentId();
            if (pid == null) {
                roots.add(c);
            } else {
                childrenBucket.computeIfAbsent(pid, k -> new ArrayList<>()).add(c);
            }
        }

        return roots.stream()
                .map(c -> fillChildren(c, childrenBucket))
                .toList();
    }

    /**
     * DFS로 하위 노드를 채우고, children/hasChildren만 보강한다.
     */
    private CommentDTO fillChildren(CommentDTO node, Map<String, List<CommentDTO>> bucket) {
        List<CommentDTO> rawChildren = bucket.getOrDefault(node.getCommentId(), List.of());
        List<CommentDTO> filled = rawChildren.stream()
                .map(ch -> fillChildren(ch, bucket))
                .toList();

        return dtoMapper.mergeFlatDto(node, filled);
    }
}
