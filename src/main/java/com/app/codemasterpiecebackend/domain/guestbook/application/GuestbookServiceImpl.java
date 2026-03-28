package com.app.codemasterpiecebackend.domain.guestbook.application;

import com.app.codemasterpiecebackend.domain.guestbook.dto.EntryDTO;
import com.app.codemasterpiecebackend.domain.guestbook.entity.GuestbookEntry;
import com.app.codemasterpiecebackend.domain.guestbook.repository.GuestbookRepository;
import com.app.codemasterpiecebackend.domain.shared.embeddable.ActorSnapshot;
import com.app.codemasterpiecebackend.domain.shared.embeddable.GuestAuth;
import com.app.codemasterpiecebackend.domain.shared.security.ContentAuthorizer;
import com.app.codemasterpiecebackend.domain.shared.security.ActorProvider;
import com.app.codemasterpiecebackend.global.support.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

@Service
@RequiredArgsConstructor
public class GuestbookServiceImpl implements GuestbookService {

    @Value("${app.auth.author-github-id}")
    private String myGithubId;


    private final GuestbookRepository guestbookRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 방명록 생성
     * ANON → 게스트 인증정보 저장
     * GITHUB → GitHub 아바타 URL 생성
     */
    @Override
    @Transactional
    public EntryDTO create(GuestbookCommand.Create cmd) {
        var builder = GuestbookEntry.builder()
                .content(cmd.content())
                .actorProvider(cmd.actor().provider())
                .actorId(cmd.actor().actorId())
                .actorSnapshot(ActorSnapshot.builder()
                        .displayName(cmd.displayName())
                        .imageUrl(cmd.avatarUrl())
                        .build());

        // 익명 사용자일 경우에만 보안 정보(PIN) 추가 저장
        if (cmd.actor().provider() == ActorProvider.ANON) {
            builder.guestAuth(GuestAuth.builder()
                    .pinHash(passwordEncoder.encode(cmd.guest().pin()))
                    .build());
        }

        GuestbookEntry saved = guestbookRepository.save(builder.build());
        return GuestbookDTOMapper.toDto(saved, myGithubId);
    }

    /**
     * 커서 기반 슬라이스 조회 (createdAt,id DESC)
     */
    @Override
    @Transactional(readOnly = true)
    public Slice<EntryDTO> getSlice(GuestbookCommand.Slice cmd) {
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
                rows.stream().map((r) -> GuestbookDTOMapper.toDto(r, myGithubId)).toList(),
                PageRequest.of(0, size),
                hasNext
        );
    }

    /**
     * 권한 체크 후 내용 수정
     */
    @Override
    @Transactional
    public EntryDTO update(GuestbookCommand.Update cmd) {
        GuestbookEntry entry = findEntryOr404(cmd.entryId());

        ensureModifiable(entry, cmd.userId(), cmd.password(), cmd.elevated());

        entry.updateContent(cmd.content());
        return GuestbookDTOMapper.toDto(entry, myGithubId);
    }

    /**
     * 권한 체크 후 삭제
     */
    @Override
    @Transactional
    public void delete(GuestbookCommand.Delete cmd) {
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
     * 수정/삭제 권한 검증.
     * <ul>
     * <li>관리자(elevated=true) -> 무조건 통과</li>
     * <li>소셜 로그인(GITHUB, GITLAB 등) -> 본인(actorId 일치)만 통과</li>
     * <li>익명(ANON) -> PIN 번호 일치 시 통과</li>
     * </ul>
     */
    private void ensureModifiable(GuestbookEntry entry, String userId, String password, boolean elevated) {
        ContentAuthorizer.verifyOwnership(
                elevated,
                entry.getActorProvider(),
                entry.getActorId(),
                entry.getGuestAuth(),
                userId,
                password,
                passwordEncoder,
                "validation.guestbook.pin.invalid"
        );
    }
}

