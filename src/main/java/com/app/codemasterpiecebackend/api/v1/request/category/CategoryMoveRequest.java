package com.app.codemasterpiecebackend.api.v1.request.category;

import com.app.codemasterpiecebackend.service.category.cmd.MoveCategoryCmd;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CategoryMoveRequest(

        @NotBlank(message = "validation.category.id.notBlank")
        String categoryId,

        String newParentId,

        @PositiveOrZero(message = "validation.category.move.indexPositiveOrZero")
        Integer newIndex,

        String beforeId,
        String afterId
) {
    public MoveCategoryCmd toCmd() {
        return new MoveCategoryCmd(categoryId, newParentId, newIndex, beforeId, afterId);
    }
}
