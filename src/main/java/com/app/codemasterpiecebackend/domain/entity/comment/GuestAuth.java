package com.app.codemasterpiecebackend.domain.entity.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class GuestAuth {
    @Column(name = "guest_pin_hash", length = 100) // bcrypt/argon 여유분
    private String pinHash;
}
