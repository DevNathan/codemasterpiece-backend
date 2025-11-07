package com.app.codemasterpiecebackend.mapper;

import com.app.codemasterpiecebackend.domain.dto.post.PostDetailDTO;
import com.app.codemasterpiecebackend.domain.dto.post.PostListDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PostMapper {

    long countPostPage(
            @Param("link") String link,
            @Param("keyword") String keyword,
            @Param("elevated") boolean elevated
    );

    List<PostListDTO> findPostPage(
            @Param("link") String link,
            @Param("keyword") String keyword,
            @Param("elevated") boolean elevated,
            @Param("sortKey") String sortKey,
            @Param("sortDir") String sortDir,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    Optional<PostDetailDTO> findPostDetail(
            @Param("slug") String slug,
            @Param("actorProvider") String actorProvider,
            @Param("actorId") String actorId,
            @Param("elevated") boolean elevated
    );
}
