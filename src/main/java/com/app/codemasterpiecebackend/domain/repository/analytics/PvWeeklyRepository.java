package com.app.codemasterpiecebackend.domain.repository.analytics;

import com.app.codemasterpiecebackend.domain.entity.analytics.PvWeekly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PvWeeklyRepository extends JpaRepository<PvWeekly, LocalDate> {
}