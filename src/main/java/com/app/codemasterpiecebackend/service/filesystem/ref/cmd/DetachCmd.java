package com.app.codemasterpiecebackend.service.filesystem.ref.cmd;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record DetachCmd(
        String ownerId,
        String fileId
) {
    public DetachCmd(String ownerId, String fileId) {
        this.ownerId = trimToNull(ownerId);
        this.fileId = trimToNull(fileId);
    }
}
