package com.app.codemasterpiecebackend.mapper;

import com.app.codemasterpiecebackend.domain.dto.comment.CommentDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {
    List<CommentDTO> findCommentsByPostId(
            @Param("postId") String postId,
            @Param("elevated") boolean elevated,
            @Param("actorId") String actorId,
            @Param("limit") int limit,
            @Param("offset") long offset
    );

    long countCommentsByPostId(
            @Param("postId") String postId
    );
}
