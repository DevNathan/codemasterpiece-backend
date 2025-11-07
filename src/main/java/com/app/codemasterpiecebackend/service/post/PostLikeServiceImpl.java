package com.app.codemasterpiecebackend.service.post;

import com.app.codemasterpiecebackend.domain.dto.post.PostLikeResultDTO;
import com.app.codemasterpiecebackend.domain.entity.post.Post;
import com.app.codemasterpiecebackend.domain.entity.post.PostLike;
import com.app.codemasterpiecebackend.domain.repository.PostLikeRepository;
import com.app.codemasterpiecebackend.domain.repository.PostRepository;
import com.app.codemasterpiecebackend.service.post.cmd.PostLikeCmd;
import com.app.codemasterpiecebackend.support.exception.AppException;
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
    public PostLikeResultDTO toggle(PostLikeCmd cmd) {
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
                    delta = +1;
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

        return new PostLikeResultDTO(wantLike, likeCount);
    }
}
