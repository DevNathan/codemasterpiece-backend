package com.app.codemasterpiecebackend.service.filesystem.ref.cmd;

import com.app.codemasterpiecebackend.domain.entity.file.FileOwnerType;
import com.app.codemasterpiecebackend.domain.entity.file.FilePurpose;
import lombok.Builder;

@Builder
public record ListQuery(
        FileOwnerType ownerType,
        String ownerId,
        FilePurpose purpose
) {}