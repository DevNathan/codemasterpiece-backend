package com.app.codemasterpiecebackend.api.v1.controller;

import com.app.codemasterpiecebackend.domain.dto.response.SuccessPayload;
import com.app.codemasterpiecebackend.service.category.CategoryService;
import com.app.codemasterpiecebackend.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sitemaps")
@RequiredArgsConstructor
public class SitemapController {
    private final CategoryService categoryService;
    private final PostService postService;

    @GetMapping
    public SuccessPayload<?> getDynamicSitemaps() {
        var categories = categoryService.getSitemapLinks();
        var posts = postService.getSitemaps();

        return SuccessPayload.of(Map.of("categories", categories, "posts", posts));
    }
}
