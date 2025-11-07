package com.app.codemasterpiecebackend.domain.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public abstract class BaseTimeEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.onPrePersistHook();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        this.onPreUpdateHook();
    }

    /** 자식 엔티티가 필요 시 오버라이드할 수 있는 훅 */
    protected void onPrePersistHook() {}
    protected void onPreUpdateHook() {}
}
