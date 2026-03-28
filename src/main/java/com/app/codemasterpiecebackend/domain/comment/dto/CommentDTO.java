package com.app.codemasterpiecebackend.domain.comment.dto;

import com.app.codemasterpiecebackend.global.util.MarkdownUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.Instant;
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

    /**
     * 댓글 본문(Markdown)을 보안 정책이 적용된 HTML로 변환합니다.
     */
    public void parseContentToHtml() {
        if (this.content != null && !this.deleted) {
            // 댓글 전용 렌더러(보안 필터링 포함)를 사용하여 변환
            this.content = MarkdownUtil.parseCommentToHtml(this.content);
        }
    }
}
