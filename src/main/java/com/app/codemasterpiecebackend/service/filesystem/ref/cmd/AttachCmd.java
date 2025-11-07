package com.app.codemasterpiecebackend.service.filesystem.ref.cmd;

import com.app.codemasterpiecebackend.domain.entity.file.FileOwnerType;
import com.app.codemasterpiecebackend.domain.entity.file.FilePurpose;
import lombok.Builder;

@Builder
public record AttachCmd(
        String fileId,
        FileOwnerType ownerType,
        String ownerId,
        FilePurpose purpose,
        Integer sortOrder,      // null이면 자동부여(맨 뒤)
        String displayName      // 선택
) {}