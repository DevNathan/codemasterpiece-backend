package com.app.codemasterpiecebackend.service.filesystem.ref;

import com.app.codemasterpiecebackend.domain.entity.file.*;
import com.app.codemasterpiecebackend.domain.repository.file.FileRefRepository;
import com.app.codemasterpiecebackend.domain.repository.file.StoredFileRepository;
import com.app.codemasterpiecebackend.service.filesystem.ref.cmd.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FileRefServiceImpl implements FileRefService {

    // 간격을 둬서 이후 삽입 시 정렬 충돌 완화(최종 reorder로 1..N 고정)
    private static final int GAP = 10;

    private final FileRefRepository refRepo;
    private final StoredFileRepository fileRepo;

    // =============== 단일 연산들 ===============

    @Override
    public void attach(AttachCmd cmd) {
        // 멱등 보장: 이미 있으면 no-op
        boolean exists = refRepo.existsByStoredFileIdAndOwnerTypeAndOwnerIdAndPurpose(
                cmd.fileId(), cmd.ownerType(), cmd.ownerId(), cmd.purpose()
        );
        if (exists) return;

        StoredFile file = fileRepo.findById(cmd.fileId())
                .orElseThrow(() -> new IllegalArgumentException("file not found: " + cmd.fileId()));

        Integer sort = cmd.sortOrder();
        if (sort == null) {
            Integer max = refRepo.findMaxSort(cmd.ownerType(), cmd.ownerId(), cmd.purpose());
            sort = (max == null ? 0 : max) + GAP;
        }

        FileRef ref = FileRef.builder()
                .storedFile(file)
                .ownerType(cmd.ownerType())
                .ownerId(cmd.ownerId())
                .purpose(cmd.purpose())
                .sortOrder(sort)
                .displayName(cmd.displayName())
                .build();

        refRepo.save(ref);
        fileRepo.incRef(file.getId());
    }

    @Override
    public void detach(DetachCmd cmd) {
        FileRef ref = refRepo.findByOwnerIdAndStoredFileId(
                cmd.ownerId(),
                cmd.fileId()
        ).orElse(null);
        if (ref == null) return;

        String fileId = ref.getStoredFile().getId();
        refRepo.delete(ref);
        fileRepo.decRef(fileId);
    }

    @Override
    public void reorder(ReorderCmd cmd) {
        // 현재 집합 로드(동일 owner/purpose 범위)
        List<FileRef> current = refRepo.findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
                cmd.ownerType(), cmd.ownerId(), cmd.purpose()
        );
        if (current.isEmpty()) return;

        Map<String, FileRef> byRefId = current.stream()
                .collect(Collectors.toMap(FileRef::getId, x -> x));

        // 전달된 refIds가 같은 범위인지 체크(이상값은 무시)
        for (String refId : cmd.refIdsInOrder()) {
            if (!byRefId.containsKey(refId)) {
                // 필요하면 throw로 강하게 막아도 됨
                continue;
            }
        }

        int order = 1; // 최종 1..N로 고정
        for (String refId : cmd.refIdsInOrder()) {
            FileRef fr = byRefId.get(refId);
            if (fr == null) continue;
            if (!Objects.equals(fr.getSortOrder(), order)) {
                fr.setSortOrder(order);
                refRepo.save(fr);
            }
            order++;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileRefService.FileRefEntry> list(FileRefService.ListQuery q) {
        return refRepo.findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
                        q.ownerType(), q.ownerId(), q.purpose()
                ).stream()
                .map(fr -> new FileRefService.FileRefEntry(fr.getId(), fr.getStoredFile().getId(), fr.getSortOrder()))
                .toList();
    }

    // =============== 핵심: replaceAll ===============

    @Override
    public FileRefService.ReplaceAllResult replaceAll(FileRefService.ReplaceAllCmd cmd) {
        // 현재 상태(정렬 보장)
        List<FileRef> current = refRepo.findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
                cmd.ownerType(), cmd.ownerId(), cmd.purpose()
        );

        // 파일 존재 검증(추가될 후보들만)
        Set<String> desiredSet = new LinkedHashSet<>();
        for (String fid : cmd.orderedFileIds()) {
            if (fid == null || fid.isBlank()) continue;
            desiredSet.add(fid.trim());
        }
        if (!desiredSet.isEmpty()) {
            Map<String, StoredFile> exists = fileRepo.findAllById(desiredSet).stream()
                    .collect(Collectors.toMap(StoredFile::getId, x -> x));
            for (String fid : desiredSet) {
                if (!exists.containsKey(fid)) {
                    throw new IllegalArgumentException("file not found: " + fid);
                }
            }
        }

        // 현재 fileId 집합
        Map<String, FileRef> currentByFileId = new HashMap<>(current.size() * 2);
        for (FileRef fr : current) {
            currentByFileId.put(fr.getStoredFile().getId(), fr);
        }

        // 1) 삭제: OLD - NEW
        for (FileRef fr : current) {
            String fid = fr.getStoredFile().getId();
            if (!desiredSet.contains(fid)) {
                refRepo.delete(fr);
                fileRepo.decRef(fid);
            }
        }

        // 2) 추가: NEW - OLD (뒤에 붙임; 최종 정렬은 reorder에서 1..N로 확정)
        if (!desiredSet.isEmpty()) {
            Integer max = refRepo.findMaxSort(cmd.ownerType(), cmd.ownerId(), cmd.purpose());
            int base = (max == null ? 0 : max);
            int step = GAP;
            int pos = base + step;

            for (String fid : desiredSet) {
                if (currentByFileId.containsKey(fid)) continue; // 기존 존재
                StoredFile file = fileRepo.getReferenceById(fid);
                FileRef ref = FileRef.builder()
                        .storedFile(file)
                        .ownerType(cmd.ownerType())
                        .ownerId(cmd.ownerId())
                        .purpose(cmd.purpose())
                        .sortOrder(pos)
                        .displayName(cmd.displayNamePrefix() == null ? null : (cmd.displayNamePrefix() + ""))
                        .build();
                refRepo.save(ref);
                fileRepo.incRef(fid);
                pos += step;
            }
        }

        // 3) 최종 순서 고정
        List<FileRef> fresh = refRepo.findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
                cmd.ownerType(), cmd.ownerId(), cmd.purpose()
        );
        Map<String, FileRef> byFileId = fresh.stream()
                .collect(Collectors.toMap(fr -> fr.getStoredFile().getId(), x -> x));

        List<String> refIdsInOrder = new ArrayList<>(desiredSet.size());
        for (String fid : desiredSet) {
            FileRef fr = byFileId.get(fid);
            if (fr != null) refIdsInOrder.add(fr.getId());
        }
        if (!refIdsInOrder.isEmpty()) {
            reorder(ReorderCmd.builder()
                    .ownerType(cmd.ownerType())
                    .ownerId(cmd.ownerId())
                    .purpose(cmd.purpose())
                    .refIdsInOrder(refIdsInOrder)
                    .build());
        }

        // 최종 상태 반환
        List<FileRef> finalList = refRepo.findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
                cmd.ownerType(), cmd.ownerId(), cmd.purpose()
        );
        List<FileRefService.FileRefEntry> out = finalList.stream()
                .map(fr -> new FileRefService.FileRefEntry(fr.getId(), fr.getStoredFile().getId(), fr.getSortOrder()))
                .toList();

        return new FileRefService.ReplaceAllResult(out);
    }

    // =============== 일괄 해제: detachAll ===============

    @Override
    public void detachAll(FileOwnerType ownerType, String ownerId) {
        // (필요 시) 레포에 아래 메서드 추가: findByOwnerTypeAndOwnerId(...)
        List<FileRef> refs = refRepo.findByOwnerTypeAndOwnerId(ownerType, ownerId);
        if (refs.isEmpty()) return;

        // 파일별 감소량 집계
        Map<String, Integer> decCount = new HashMap<>();
        for (FileRef r : refs) {
            String fid = r.getStoredFile().getId();
            decCount.merge(fid, 1, Integer::sum);
        }

        // REF 선삭제(단건 detach와 동일한 순서 유지)
        refRepo.deleteAllInBatch(refs);

        // ref_count 감소(레포의 decRef가 멱등/경쟁안전하다는 전제)
        for (Map.Entry<String, Integer> e : decCount.entrySet()) {
            String fid = e.getKey();
            int n = e.getValue();
            for (int i = 0; i < n; i++) {
                fileRepo.decRef(fid);
            }
        }
    }

    @Override
    public void detachAll(FileOwnerType ownerType, String ownerId, FilePurpose purpose) {
        List<FileRef> refs = refRepo.findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
                ownerType, ownerId, purpose
        );
        if (refs.isEmpty()) return;

        Map<String, Integer> decCount = new HashMap<>();
        for (FileRef r : refs) {
            String fid = r.getStoredFile().getId();
            decCount.merge(fid, 1, Integer::sum);
        }

        refRepo.deleteAllInBatch(refs);

        for (Map.Entry<String, Integer> e : decCount.entrySet()) {
            String fid = e.getKey();
            int n = e.getValue();
            for (int i = 0; i < n; i++) {
                fileRepo.decRef(fid);
            }
        }
    }
}
