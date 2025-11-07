package com.app.codemasterpiecebackend.domain.entity.analytics;

import com.app.codemasterpiecebackend.domain.entity.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 모든 롤업 엔티티의 공통 베이스.
 * - keyDate는 "UTC 기준 날짜"를 의미한다. (일/주/월 모두 UTC 시작일)
 * - 원본 이벤트 타임라인은 Instant(UTC)로 관리한다.
 */
@MappedSuperclass
@Getter
public abstract class BaseRollup extends BaseTimeEntity {

    /** UTC 기준의 키 날짜 (DAY=해당 UTC day, WEEK/MONTH=UTC 시작일) */
    @Id
    @Column(name = "key_date", nullable = false)
    protected LocalDate keyDate;

    @Column(nullable = false)
    protected long views;

    @Column(nullable = false)
    protected long uv;

    @Column(nullable = false)
    protected long sessions;
}
