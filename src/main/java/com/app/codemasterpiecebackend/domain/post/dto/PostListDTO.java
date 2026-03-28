package com.app.codemasterpiecebackend.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostListDTO {
    private String postId;
    private String slug;
    private String title;
    private String categoryName;
    private String headImage;
    private String headContent;
    private long viewCount;
    private long likeCount;
    private boolean published;
    private Instant createdAt;
    private Instant updatedAt;

    private List<String> tags;
}
