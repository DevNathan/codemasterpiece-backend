package com.app.codemasterpiecebackend.domain.pageView.repository;

import com.app.codemasterpiecebackend.domain.pageView.entity.PvWeekly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PvWeeklyRepository extends JpaRepository<PvWeekly, LocalDate> {
}