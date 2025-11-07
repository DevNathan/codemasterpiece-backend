package com.app.codemasterpiecebackend.domain.entity.analytics;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 주간 롤업: keyDate = 주 시작일(월요일, ISO) YYYY-MM-DD
 */
@Entity
@Table(name = "tbl_pv_weekly")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PvWeekly extends BaseRollup {

    private PvWeekly(LocalDate weekStart, long views, long uv, long sessions) {
        this.keyDate = weekStart;
        this.views = views;
        this.uv = uv;
        this.sessions = sessions;
    }

    public static PvWeekly of(LocalDate weekStart, long views, long uv, long sessions) {
        return new PvWeekly(weekStart, views, uv, sessions);
    }

    public void setRollup(long views, long uv, long sessions) {
        this.views = views;
        this.uv = uv;
        this.sessions = sessions;
    }
}
