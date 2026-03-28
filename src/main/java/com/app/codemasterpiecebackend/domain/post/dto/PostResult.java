package com.app.codemasterpiecebackend.domain.post.dto;

import lombok.Builder;

import java.time.Instant;

/**
 * 게시글(Post) 도메인의 단편적인 응답 결과를 묶어두는 통합 결과 클래스.
 */
public final class PostResult {

    private PostResult() {
    }

    @Builder
    public record Update(
            String postId,
            String slug
    ) {
    }

    public record Like(
            boolean liked,
            int likeCount
    ) {
    }

    public record Sitemap(
            String slug,
            Instant updatedAt
    ) {
    }

    public record Toc(
            String id,
            String text,
            int depth
    ) {
    }
}