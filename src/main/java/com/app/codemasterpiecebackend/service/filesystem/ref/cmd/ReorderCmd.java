package com.app.codemasterpiecebackend.service.filesystem.ref.cmd;

import com.app.codemasterpiecebackend.domain.entity.file.FileOwnerType;
import com.app.codemasterpiecebackend.domain.entity.file.FilePurpose;
import lombok.Builder;

import java.util.List;

@Builder
public record ReorderCmd(
        FileOwnerType ownerType,
        String ownerId,
        FilePurpose purpose,
        List<String> refIdsInOrder // 이 순서대로 재정렬
) {}