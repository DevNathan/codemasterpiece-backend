package com.app.codemasterpiecebackend.domain.post.application;

import com.app.codemasterpiecebackend.domain.post.dto.PostResult;
import com.app.codemasterpiecebackend.domain.post.entity.Post;
import com.app.codemasterpiecebackend.domain.post.entity.PostLike;
import com.app.codemasterpiecebackend.domain.post.repository.PostLikeRepository;
import com.app.codemasterpiecebackend.domain.post.repository.PostRepository;
import com.app.codemasterpiecebackend.global.support.exception.AppException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class PostLikeServiceImpl implements PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    @Override
    public PostResult.Like toggle(PostCommand.Like cmd) {
        Post refPost = postRepository.findById(cmd.postId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.post.not_found"));

        boolean exists = postLikeRepository.existsByPost_IdAndActorProviderAndActorId(
                cmd.postId(), cmd.provider(), cmd.actorId()
        );

        // null → 토글, true/false → 강제지정
        boolean wantLike = (cmd.toggleTo() != null)
                ? cmd.toggleTo()
                : !exists;  // null이면 현재 반대 상태로

        int delta = 0;

        if (wantLike) {
            if (!exists) {
                try {
                    postLikeRepository.save(
                            PostLike.builder()
                                    .post(refPost)
                                    .actorProvider(cmd.provider())
                                    .actorId(cmd.actorId())
                                    .build()
                    );
                    delta = 1;
                } catch (DataIntegrityViolationException ignore) {
                    // 경쟁 상태에서 중복 insert → 무시 (이미 liked)
                }
            }
        } else {
            if (exists) {
                long removed = postLikeRepository.deleteActorLike(
                        cmd.postId(), cmd.provider(), cmd.actorId()
                );
                if (removed > 0) delta = -1;
            }
        }

        if (delta != 0) {
            postRepository.bumpLikeCount(cmd.postId(), delta);
        }

        Integer lc = postRepository.findLikeCountOnly(cmd.postId());
        int likeCount = (lc != null) ? lc : 0;

        return new PostResult.Like(wantLike, likeCount);
    }
}
