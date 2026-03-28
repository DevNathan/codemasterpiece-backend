package com.app.codemasterpiecebackend.domain.post.application;

import com.app.codemasterpiecebackend.domain.shared.security.ActorProvider;
import com.app.codemasterpiecebackend.global.util.Stringx;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Locale;

import static com.app.codemasterpiecebackend.global.util.Stringx.trimToNull;

/**
 * 게시글(Post) 도메인의 모든 유즈케이스 입력을 정의하는 통합 Command 클래스.
 */
public final class PostCommand {
    private PostCommand() {
    }

    public record Create(
            String title,
            String headImageId,
            String headContent,
            List<String> tags,
            String categoryId,
            String mainContent,
            boolean published
    ) {
        public Create(
                String title,
                String headImageId,
                String headContent,
                List<String> tags,
                String categoryId,
                String mainContent,
                boolean published
        ) {
            this.title = trimToNull(title);
            this.headImageId = trimToNull(headImageId);
            this.headContent = trimToNull(headContent);
            this.categoryId = trimToNull(categoryId);
            this.mainContent = trimToNull(mainContent);
            this.published = published;

            this.tags = (tags == null) ? null :
                    tags.stream()
                            .map(Stringx::trimToNull)
                            .filter(Objects::nonNull)
                            .map(String::toLowerCase)
                            .toList();
        }
    }

    public record Update(
            String postId,
            String title,
            String headImageId,
            String headContent,
            List<String> tags,
            String categoryId,
            String mainContent,
            boolean published
    ) {
        public Update(
                String postId,
                String title,
                String headImageId,
                String headContent,
                List<String> tags,
                String categoryId,
                String mainContent,
                boolean published
        ) {
            this.postId = trimToNull(postId);
            this.title = trimToNull(title);
            this.headImageId = trimToNull(headImageId);
            this.headContent = trimToNull(headContent);
            this.categoryId = trimToNull(categoryId);

            this.tags = (tags == null)
                    ? List.of()
                    : tags.stream()
                    .map(Stringx::trimToNull)
                    .filter(Objects::nonNull)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .toList();

            this.mainContent = (mainContent == null) ? "" : mainContent;
            this.published = published;
        }
    }

    public record Detail(
            String slug,
            ActorProvider actorProvider,
            String actorId,
            boolean elevated,
            boolean excludeContent
    ) {
    }

    public record Search(
            Pageable pageable,
            boolean elevated,
            String link,
            String keyword
    ) {
        public Search(Pageable pageable, boolean elevated, String link, String keyword) {
            this.pageable = pageable;
            this.elevated = elevated;
            this.link = trimToNull(link);
            this.keyword = trimToNull(keyword);
        }
    }

    public record Like(
            String postId,
            ActorProvider provider,
            String actorId,
            @Nullable Boolean toggleTo // null이면 토글, true=좋아요, false=취소
    ) {
    }
}