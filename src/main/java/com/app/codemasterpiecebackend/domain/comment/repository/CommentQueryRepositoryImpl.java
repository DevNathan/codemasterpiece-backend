package com.app.codemasterpiecebackend.domain.comment.repository;

import com.app.codemasterpiecebackend.domain.comment.dto.CommentDTO;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
class CommentQueryRepositoryImpl implements CommentQueryRepository {

    private final CommentMapper commentMapper;

    @Override
    public List<CommentDTO> findCommentsByPostId(String postId, boolean elevated, String actorId, int limit, long offset) {
        return commentMapper.findCommentsByPostId(postId, elevated, actorId, limit, offset);
    }

    @Override
    public long countCommentsByPostId(String postId) {
        return commentMapper.countCommentsByPostId(postId);
    }
}