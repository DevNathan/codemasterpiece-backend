package com.app.codemasterpiecebackend.domain.post.repository;

import com.app.codemasterpiecebackend.domain.post.dto.PostDetailDTO;
import com.app.codemasterpiecebackend.domain.post.dto.PostListDTO;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
class PostQueryRepositoryImpl implements PostQueryRepository {

    private final PostMapper postMapper;

    @Override
    public long countPostPage(String link, String keyword, boolean elevated) {
        return postMapper.countPostPage(link, keyword, elevated);
    }

    @Override
    public List<PostListDTO> findPostPage(String link, String keyword, boolean elevated, String sortKey, String sortDir, int limit, int offset) {
        return postMapper.findPostPage(link, keyword, elevated, sortKey, sortDir, limit, offset);
    }

    @Override
    public Optional<PostDetailDTO> findPostDetail(String slug, String actorProvider, String actorId, boolean elevated, boolean excludeContent) {
        return postMapper.findPostDetail(slug, actorProvider, actorId, elevated, excludeContent);
    }
}