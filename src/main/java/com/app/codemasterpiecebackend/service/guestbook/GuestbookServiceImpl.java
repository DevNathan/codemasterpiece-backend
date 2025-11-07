package com.app.codemasterpiecebackend.service.guestbook;

import com.app.codemasterpiecebackend.domain.dto.guestbook.EntryDTO;
import com.app.codemasterpiecebackend.domain.entity.comment.ActorSnapshot;
import com.app.codemasterpiecebackend.domain.entity.comment.GuestAuth;
import com.app.codemasterpiecebackend.domain.entity.guestbook.GuestbookEntry;
import com.app.codemasterpiecebackend.domain.repository.GuestbookRepository;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntryCreateCommand;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntryDeleteCmd;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntrySliceCommand;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntryUpdateCmd;
import com.app.codemasterpiecebackend.service.guestbook.mapper.GuestbookMapper;
import com.app.codemasterpiecebackend.support.exception.AppException;
import com.app.codemasterpiecebackend.support.exception.FieldValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class GuestbookServiceImpl implements GuestbookService {

    private final GuestbookRepository guestbookRepository;
    private final GuestbookMapper mapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 방명록 생성
     * ANON → 게스트 인증정보 저장
     * GITHUB → GitHub 아바타 URL 생성
     */
    @Override
    public EntryDTO create(EntryCreateCommand cmd) {
        var builder = GuestbookEntry.builder()
                .content(cmd.content());

        // 익명 사용자
        if (cmd.actor().provider() == ActorProvider.ANON) {
            var g = cmd.guest();
            builder.actorProvider(ActorProvider.ANON)
                    .actorId(cmd.actor().actorId())
                    .actorSnapshot(ActorSnapshot.builder()
                            .displayName(cmd.displayName())
                            .imageUrl(g.imageUrl())
                            .build())
                    .guestAuth(GuestAuth.builder()
                            .pinHash(passwordEncoder.encode(g.pin()))
                            .build());
        }
        // GitHub 로그인 사용자
        else {
            builder.actorProvider(ActorProvider.GITHUB)
                    .actorId(cmd.actor().actorId())
                    .actorSnapshot(ActorSnapshot.builder()
                            .displayName(cmd.displayName())
                            .imageUrl("https://avatars.githubusercontent.com/u/" + cmd.actor().actorId() + "?v=4")
                            .build());
        }

        GuestbookEntry saved = guestbookRepository.save(builder.build());
        return mapper.toDto(saved);
    }

    /**
     * 커서 기반 슬라이스 조회 (createdAt,id DESC)
     */
    @Override
    @Transactional(readOnly = true)
    public Slice<EntryDTO> getSlice(EntrySliceCommand cmd) {
        // 커서 디코드
        LocalDateTime lastAt = null;
        String lastId = null;

        if (cmd.cursor() != null && !cmd.cursor().isBlank()) {
            var decoded = new String(Base64.getUrlDecoder().decode(cmd.cursor()), StandardCharsets.UTF_8);
            var parts = decoded.split("\\|", 2);
            if (parts.length == 2) {
                long epochMs = Long.parseLong(parts[0]);
                lastAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneOffset.UTC);
                lastId = parts[1];
            }
        }

        int size = cmd.safeSize();

        List<GuestbookEntry> rows = guestbookRepository.findSlice(
                lastId != null,
                lastAt,
                lastId,
                PageRequest.of(0, size + 1, Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                ))
        );

        boolean hasNext = rows.size() > size;
        if (hasNext) rows = rows.subList(0, size);

        return new SliceImpl<>(
                rows.stream().map(mapper::toDto).toList(),
                PageRequest.of(0, size),
                hasNext
        );
    }

    /**
     * 권한 체크 후 내용 수정
     */
    @Override
    public EntryDTO update(EntryUpdateCmd cmd) {
        GuestbookEntry entry = findEntryOr404(cmd.entryId());

        ensureModifiable(entry, cmd.userId(), cmd.password(), cmd.elevated());

        entry.updateContent(cmd.content());
        return mapper.toDto(entry);
    }

    /**
     * 권한 체크 후 삭제
     */
    @Override
    public void delete(EntryDeleteCmd cmd) {
        GuestbookEntry entry = findEntryOr404(cmd.entryId());

        ensureModifiable(entry, cmd.userId(), cmd.password(), cmd.elevated());

        guestbookRepository.delete(entry);
    }

    // ===== 내부 헬퍼 =====

    private GuestbookEntry findEntryOr404(String id) {
        return guestbookRepository.findById(id).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "error.guestbook.entry_not_found")
        );
    }

    /**
     * 권한 보장:
     * - elevated == true → 통과
     * - actorId 동일 → 통과
     * - 게스트 비번 일치 → 통과
     * 조건 미충족 시 403 예외.
     */
    private void ensureModifiable(GuestbookEntry entry, String userId, String password, boolean elevated) {
        final ActorProvider provider = entry.getActorProvider();

        // 1) 관리자(또는 elevated 권한)면 바로 통과
        if (elevated) return;

        // 2) GitHub 작성물: 소유자만 허용
        if (provider == ActorProvider.GITHUB) {
            boolean isOwner = entry.getActorId() != null && entry.getActorId().equals(userId);
            if (isOwner) return;
            throw new AppException(HttpStatus.FORBIDDEN, "error.forbidden");
        }

        // 3) 익명 작성물: PIN 일치해야 허용
        if (provider == ActorProvider.ANON) {
            boolean pinOk =
                    entry.getGuestAuth() != null &&
                            password != null &&
                            passwordEncoder.matches(password, entry.getGuestAuth().getPinHash());
            if (pinOk) return;

            // PIN 불일치(또는 미제공) → 필드 단위 밸리데이션 에러로 반환
            throw new FieldValidationException(Map.of("guestPassword", "validation.guestbook.pin.invalid"));
        }

        // 4) 정의되지 않은 provider → 차단
        throw new AppException(HttpStatus.FORBIDDEN, "error.forbidden");
    }
}

