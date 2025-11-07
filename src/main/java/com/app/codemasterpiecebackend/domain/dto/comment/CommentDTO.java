package com.app.codemasterpiecebackend.domain.dto.comment;

import com.app.codemasterpiecebackend.domain.entity.comment.ReactionValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@With
public class CommentDTO {
    private String commentId;
    private String parentId;
    private String actorId;
    private String profileImage;
    private String nickname;
    private String content;
    private int reaction;
    private String myReaction;
    private int depth;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean hidden;
    private boolean deleted;
    private boolean anon;
    private boolean hasChildren;
    private List<CommentDTO> children;
}
