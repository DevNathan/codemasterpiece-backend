package com.app.codemasterpiecebackend.api.v1.controller;

import com.app.codemasterpiecebackend.api.v1.request.category.CategoryCreateRequest;
import com.app.codemasterpiecebackend.api.v1.request.category.CategoryMoveRequest;
import com.app.codemasterpiecebackend.api.v1.request.category.CategoryUpdateRequest;
import com.app.codemasterpiecebackend.domain.dto.category.CategoryDTO;
import com.app.codemasterpiecebackend.domain.dto.response.SuccessPayload;
import com.app.codemasterpiecebackend.service.category.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 카테고리 API (v1).
 * 순서: Create → Read → Update → Move(특수) → Delete
 */
@RestController
@RequestMapping(
        value = "/api/v1/categories",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ===== CREATE =====

    /**
     * 카테고리 생성 (이미지 포함 가능).
     * multipart/form-data로 폼 필드 + 파일 업로드 처리.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AUTHOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessPayload<Void> createCategory(
            @Valid @ModelAttribute CategoryCreateRequest body
    ) {
        categoryService.create(body.toCmd());
        return SuccessPayload.msg("success.category.created");
    }

    // ===== READ =====

    /**
     * 카테고리 트리 조회.
     */
    @GetMapping
    public SuccessPayload<List<CategoryDTO>> getCategoryTree() {
        return SuccessPayload.of(categoryService.getTree());
    }

    // ===== UPDATE =====

    /**
     * 카테고리 업데이트 (이름/링크/아이콘 교체/삭제 등).
     * multipart/form-data: 파일 교체 시 사용.
     */
    @PatchMapping(value = "/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<Void> updateCategory(
            @PathVariable String categoryId,
            @Valid @ModelAttribute CategoryUpdateRequest body
    ) {
        categoryService.update(body.toCmd(categoryId));
        return SuccessPayload.msg("success.category.updated");
    }

    /**
     * 카테고리 이동 (부모/순서 변경).
     * JSON 바디로 인덱스/앞뒤 기준/새 부모 전달.
     */
    @PatchMapping(value = "/move", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<Void> moveCategory(@RequestBody @Valid CategoryMoveRequest body) {
        categoryService.move(body.toCmd());
        return SuccessPayload.msg("success.category.moved");
    }

    // ===== DELETE =====

    /**
     * 카테고리 삭제.
     */
    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<Void> deleteCategory(@PathVariable String categoryId) {
        categoryService.delete(categoryId);
        return SuccessPayload.msg("success.category.deleted");
    }
}
