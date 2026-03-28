package com.app.codemasterpiecebackend.domain.post.dto;

import com.app.codemasterpiecebackend.global.util.MarkdownUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDetailDTO {
    private String postId;
    private String slug;
    private String title;
    private String headImage;
    private String headContent;
    private String categoryName;
    private String categoryLink;
    private String mainContent;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean published;

    private Long viewCount;
    private Long likeCount;
    private Long commentCount;

    private boolean liked;

    private List<String> tags;

    private List<PostResult.Toc> toc = new ArrayList<>();

    private List<PostListDTO> morePosts = new ArrayList<>();

    /**
     * 조회된 본문(Markdown)을 HTML로 변환
     */
    public void parseMainContentToHtml() {
        if (this.mainContent != null) {
            // 1. 원본 마크다운 상태일 때 TOC(목차)를 먼저 추출
            this.toc = MarkdownUtil.extractToc(this.mainContent);
            // 2. 그 다음 본문을 HTML로 렌더링
            this.mainContent = MarkdownUtil.parsePostToHtml(this.mainContent);
        }
    }
}
