package com.app.codemasterpiecebackend.service.post.cmd;

import com.app.codemasterpiecebackend.domain.types.ActorProvider;

public record PostDetailCmd(
        String slug,
        ActorProvider actorProvider,
        String actorId,
        boolean elevated
) {
}
