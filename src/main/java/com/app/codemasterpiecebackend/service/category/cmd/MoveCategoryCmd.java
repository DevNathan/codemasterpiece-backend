package com.app.codemasterpiecebackend.service.category.cmd;

import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

public record MoveCategoryCmd(
        String categoryId,
        String newParentId,
        Integer newIndex,
        String beforeId,
        String afterId
) {
    public MoveCategoryCmd(String categoryId, String newParentId, Integer newIndex, String beforeId, String afterId) {
        this.categoryId = trimToNull(categoryId);
        this.newParentId = trimToNull(newParentId);
        this.newIndex = newIndex;
        this.beforeId = trimToNull(beforeId);
        this.afterId = trimToNull(afterId);
    }
}