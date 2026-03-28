package com.app.codemasterpiecebackend.domain.seo.api.v1;

import com.app.codemasterpiecebackend.global.support.response.SuccessPayload;
import com.app.codemasterpiecebackend.domain.post.application.PostService;
import com.app.codemasterpiecebackend.domain.category.application.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sitemaps")
@RequiredArgsConstructor
public class SitemapV1Controller {
    private final CategoryService categoryService;
    private final PostService postService;

    @GetMapping
    public SuccessPayload<?> getDynamicSitemaps() {
        var categories = categoryService.getSitemapLinks();
        var posts = postService.getSitemaps();

        return SuccessPayload.of(Map.of("categories", categories, "posts", posts));
    }
}
