package com.app.codemasterpiecebackend.service.post.cmd;

import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import org.springframework.lang.Nullable;

public record PostLikeCmd(
        String postId,
        ActorProvider provider,
        String actorId,
        @Nullable Boolean toggleTo // null이면 토글, true=좋아요, false=취소
) {}