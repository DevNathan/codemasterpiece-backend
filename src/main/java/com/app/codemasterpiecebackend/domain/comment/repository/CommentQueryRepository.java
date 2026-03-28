package com.app.codemasterpiecebackend.domain.comment.repository;

import com.app.codemasterpiecebackend.domain.comment.dto.CommentDTO;

import java.util.List;

interface CommentQueryRepository {
    List<CommentDTO> findCommentsByPostId(String postId, boolean elevated, String actorId, int limit, long offset);

    long countCommentsByPostId(String postId);
}