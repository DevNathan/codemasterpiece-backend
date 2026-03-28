package com.app.codemasterpiecebackend.domain.post.repository;

import com.app.codemasterpiecebackend.domain.post.dto.PostDetailDTO;
import com.app.codemasterpiecebackend.domain.post.dto.PostListDTO;

import java.util.List;
import java.util.Optional;

interface PostQueryRepository {
    long countPostPage(String link, String keyword, boolean elevated);

    List<PostListDTO> findPostPage(String link, String keyword, boolean elevated, String sortKey, String sortDir, int limit, int offset);

    Optional<PostDetailDTO> findPostDetail(String slug, String actorProvider, String actorId, boolean elevated, boolean excludeContent);
}