package com.app.codemasterpiecebackend.domain.pageView.repository;

import com.app.codemasterpiecebackend.domain.pageView.entity.PvDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PvDailyRepository extends JpaRepository<PvDaily, LocalDate> {
}
