package com.app.codemasterpiecebackend.domain.repository;

import com.app.codemasterpiecebackend.domain.entity.post.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {
    List<Tag> findByNameIn(Collection<String> names);
}
