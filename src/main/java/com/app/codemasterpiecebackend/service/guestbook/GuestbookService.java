package com.app.codemasterpiecebackend.service.guestbook;

import com.app.codemasterpiecebackend.domain.dto.guestbook.EntryDTO;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntryCreateCommand;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntryDeleteCmd;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntrySliceCommand;
import com.app.codemasterpiecebackend.service.guestbook.cmd.EntryUpdateCmd;
import org.springframework.data.domain.Slice;

/**
 * 방명록 도메인 서비스.
 *
 * <p>쓰기 작업: 생성 / 수정 / 삭제 (권한 검증 포함)
 * <br>읽기 작업: 커서 기반 페이징 조회
 */
public interface GuestbookService {

    /**
     * 방명록 엔트리를 생성한다.
     *
     * @param cmd 생성 커맨드 (작성자 정보, 내용, 게스트 인증 등)
     * @return 생성된 엔트리 DTO
     */
    EntryDTO create(EntryCreateCommand cmd);

    /**
     * 방명록 엔트리를 커서 기반 슬라이스로 조회한다.
     *
     * @param cmd 슬라이스 조회 커맨드 (cursor, size)
     * @return 엔트리 DTO 슬라이스
     */
    Slice<EntryDTO> getSlice(EntrySliceCommand cmd);

    /**
     * 엔트리 내용을 수정한다.
     * 권한 조건:
     * <ul>
     *   <li>관리자 권한(elevated == true)</li>
     *   <li>작성자 본인(entry.actorId == cmd.userId)</li>
     *   <li>게스트 비밀번호 일치</li>
     * </ul>
     *
     * @param cmd 수정 커맨드
     * @return 수정된 엔트리 DTO
     */
    EntryDTO update(EntryUpdateCmd cmd);

    /**
     * 엔트리를 삭제한다.
     * 권한 규칙은 수정과 동일.
     *
     * @param cmd 삭제 커맨드
     */
    void delete(EntryDeleteCmd cmd);
}
