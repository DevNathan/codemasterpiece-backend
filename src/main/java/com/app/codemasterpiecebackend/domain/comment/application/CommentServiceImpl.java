package com.app.codemasterpiecebackend.domain.comment.application;

import com.app.codemasterpiecebackend.domain.comment.dto.CommentDTO;
import com.app.codemasterpiecebackend.domain.comment.entity.Comment;
import com.app.codemasterpiecebackend.domain.comment.entity.ReactionValue;
import com.app.codemasterpiecebackend.domain.comment.repository.CommentReactionRepository;
import com.app.codemasterpiecebackend.domain.comment.repository.CommentRepository;
import com.app.codemasterpiecebackend.domain.post.entity.Post;
import com.app.codemasterpiecebackend.domain.post.repository.PostRepository;
import com.app.codemasterpiecebackend.domain.shared.embeddable.ActorSnapshot;
import com.app.codemasterpiecebackend.domain.shared.embeddable.GuestAuth;
import com.app.codemasterpiecebackend.domain.shared.security.ContentAuthorizer;
import com.app.codemasterpiecebackend.domain.shared.security.ActorProvider;
import com.app.codemasterpiecebackend.global.support.exception.AppException;
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
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    // ===== C(reate) =====

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CommentDTO create(CommentCommand.Create cmd) {
        final Post postRef = postRepository.getReferenceById(cmd.postId());

        int depth = 0;
        Comment parentRef = null;
        if (cmd.parentId() != null) {
            var briefOpt = commentRepository.findParentBrief(cmd.parentId());
            int parentDepth = briefOpt.map(CommentRepository.ParentBrief::getDepth).orElse(0);
            depth = parentDepth + 1;
            parentRef = commentRepository.getReferenceById(cmd.parentId());
        }

        Comment.CommentBuilder builder = Comment.builder()
                .post(postRef)
                .content(cmd.content())
                .parent(parentRef)
                .depth(depth)
                .actorProvider(cmd.actor().provider())
                .actorId(cmd.actor().actorId())
                .actorSnapshot(
                        ActorSnapshot.builder()
                                .displayName(cmd.displayName())
                                .imageUrl(cmd.avatarUrl())
                                .build()
                );

        // 익명 사용자일 경우에만 PIN 저장 로직 수행
        if (cmd.actor().provider() == ActorProvider.ANON) {
            builder.guestAuth(
                    GuestAuth.builder()
                            .pinHash(passwordEncoder.encode(cmd.guest().pin()))
                            .build()
            );
        }

        Comment saved = commentRepository.save(builder.build());
        return CommentDTOMapper.toDtoBasic(saved);
    }

    // ===== R(ead) =====

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CommentDTO> getPageByPostId(CommentCommand.View cmd) {
        int limit = cmd.pageable().getPageSize();
        long offset = cmd.pageable().getOffset();

        List<CommentDTO> flat = commentRepository.findCommentsByPostId(
                cmd.postId(),
                cmd.elevated(),
                cmd.actorId(),
                limit,
                offset
        );
        flat.forEach(CommentDTO::parseContentToHtml);

        List<CommentDTO> roots = toTree(flat);

        long totalRoots = commentRepository.countCommentsByPostId(cmd.postId());
        return new PageImpl<>(roots, cmd.pageable(), totalRoots);
    }

    @Override
    @Transactional(readOnly = true)
    public String getRawContent(CommentCommand.Raw cmd) {
        Comment comment = commentRepository.findById(cmd.commentId()).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "error.comment.not_found")
        );

        // 보안 체크: GITHUB 계정은 소유자나 AUTHOR가 아니면 원본 노출 금지
        if (comment.getActorProvider() == ActorProvider.GITHUB) {
            boolean isOwner = comment.getActorId() != null && comment.getActorId().equals(cmd.actorId());
            if (!cmd.elevated() && !isOwner) {
                throw new AppException(HttpStatus.FORBIDDEN, "error.forbidden");
            }
        }

        return comment.getContent();
    }

    // ===== U(pdate) =====

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CommentDTO update(CommentCommand.Update cmd) {
        Comment comment = commentRepository.findById(cmd.commentId()).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "error.comment.not_found")
        );

        ensureModifiable(comment, cmd.userId(), cmd.password(), cmd.elevated());
        comment.updateContent(cmd.content());

        return CommentDTOMapper.toDtoBasic(comment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean toggleHide(CommentCommand.Like cmd) {
        int updated = commentRepository.updateHiddenById(cmd.commentId(), cmd.setHidden());
        return (updated > 0) == cmd.setHidden();
    }

    // ===== D(elete) =====

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(CommentCommand.Delete cmd) {
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
    @Transactional
    public @Nullable ReactionValue react(CommentCommand.React cmd) {
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
     * <li>관리자(elevated=true) -> 무조건 통과</li>
     * <li>소셜 로그인(GITHUB, GITLAB 등) -> 본인(actorId 일치)만 통과</li>
     * <li>익명(ANON) -> PIN 번호 일치 시 통과</li>
     * </ul>
     */
    private void ensureModifiable(Comment comment, String userId, String password, boolean elevated) {
        ContentAuthorizer.verifyOwnership(
                elevated,
                comment.getActorProvider(),
                comment.getActorId(),
                comment.getGuestAuth(),
                userId,
                password,
                passwordEncoder,
                "error.comment.invalid_passwd"
        );
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

        return CommentDTOMapper.mergeFlatDto(node, filled);
    }
}
