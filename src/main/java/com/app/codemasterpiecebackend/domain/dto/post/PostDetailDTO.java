package com.app.codemasterpiecebackend.domain.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
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

    private List<PostListDTO> morePosts = new ArrayList<>();
}
