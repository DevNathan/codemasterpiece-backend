package com.app.codemasterpiecebackend.domain.post.repository;

import com.app.codemasterpiecebackend.domain.post.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {
    List<Tag> findByNameIn(Collection<String> names);
}
