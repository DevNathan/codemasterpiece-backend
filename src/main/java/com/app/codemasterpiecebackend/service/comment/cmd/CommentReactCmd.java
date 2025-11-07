package com.app.codemasterpiecebackend.service.comment.cmd;

import com.app.codemasterpiecebackend.domain.entity.comment.ReactionValue;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;

public record CommentReactCmd(
        String commentId,
        ActorProvider actorProvider,
        String actorId,
        ReactionValue value // null이면 "취소" 의도
) {}