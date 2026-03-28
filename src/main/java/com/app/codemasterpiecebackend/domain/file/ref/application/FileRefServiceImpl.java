package com.app.codemasterpiecebackend.domain.file.ref.application;

import com.app.codemasterpiecebackend.domain.file.core.entity.StoredFile;
import com.app.codemasterpiecebackend.domain.file.core.repository.StoredFileRepository;
import com.app.codemasterpiecebackend.domain.file.ref.entity.FileOwnerType;
import com.app.codemasterpiecebackend.domain.file.ref.entity.FilePurpose;
import com.app.codemasterpiecebackend.domain.file.ref.entity.FileRef;
import com.app.codemasterpiecebackend.domain.file.ref.repository.FileRefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * FileRefService 구현체.
 * N+1 쿼리 방지를 위해 Batch 처리와 메모리 내 Diff 알고리즘을 강제합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FileRefServiceImpl implements FileRefService {

    private static final int GAP = 10;

    private final FileRefRepository refRepo;
    private final StoredFileRepository fileRepo;

    @Override
    public void attach(FileRefCommand.Attach cmd) {
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
    public void detach(FileRefCommand.Detach cmd) {
        FileRef ref = refRepo.findByOwnerIdAndStoredFileId(cmd.ownerId(), cmd.fileId()).orElse(null);
        if (ref == null) return;

        String fileId = ref.getStoredFile().getId();
        refRepo.delete(ref);
        fileRepo.decRef(fileId);
    }

    @Override
    public void reorder(FileRefCommand.Reorder cmd) {
        List<FileRef> current = refRepo.findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
                cmd.ownerType(), cmd.ownerId(), cmd.purpose()
        );
        if (current.isEmpty()) return;

        Map<String, FileRef> byRefId = current.stream()
                .collect(Collectors.toMap(FileRef::getId, x -> x));

        int order = 1;
        List<FileRef> toUpdate = new ArrayList<>();

        for (String refId : cmd.refIdsInOrder()) {
            FileRef fr = byRefId.get(refId);
            if (fr != null && !Objects.equals(fr.getSortOrder(), order)) {
                fr.setSortOrder(order);
                toUpdate.add(fr);
            }
            order++;
        }

        if (!toUpdate.isEmpty()) {
            refRepo.saveAll(toUpdate);
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

    @Override
    public FileRefService.ReplaceAllResult replaceAll(FileRefService.ReplaceAllCmd cmd) {
        List<FileRef> current = refRepo.findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
                cmd.ownerType(), cmd.ownerId(), cmd.purpose()
        );
        Map<String, FileRef> currentByFileId = current.stream()
                .collect(Collectors.toMap(fr -> fr.getStoredFile().getId(), fr -> fr, (existing, replacement) -> existing));

        List<String> desiredIds = cmd.orderedFileIds().stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .toList();

        List<FileRef> toDelete = new ArrayList<>();
        Map<String, Integer> decCount = new HashMap<>();

        for (FileRef fr : current) {
            if (!desiredIds.contains(fr.getStoredFile().getId())) {
                toDelete.add(fr);
                decCount.merge(fr.getStoredFile().getId(), 1, Integer::sum);
            }
        }

        // 🎯 모아서 한 방에 도륙하고 감산한다
        if (!toDelete.isEmpty()) {
            refRepo.deleteAllInBatch(toDelete);
            for (Map.Entry<String, Integer> e : decCount.entrySet()) {
                fileRepo.decRefBulk(e.getKey(), e.getValue());
            }
        }

        List<FileRef> toSave = new ArrayList<>();
        List<FileRefService.FileRefEntry> resultEntries = new ArrayList<>();
        int order = 1;

        for (String fid : desiredIds) {
            FileRef fr = currentByFileId.get(fid);
            if (fr == null) {
                fr = FileRef.builder()
                        .storedFile(fileRepo.getReferenceById(fid))
                        .ownerType(cmd.ownerType())
                        .ownerId(cmd.ownerId())
                        .purpose(cmd.purpose())
                        .sortOrder(order)
                        .displayName(cmd.displayNamePrefix() == null ? null : cmd.displayNamePrefix())
                        .build();
                fileRepo.incRef(fid);
            } else {
                fr.setSortOrder(order);
            }
            toSave.add(fr);
            resultEntries.add(new FileRefService.FileRefEntry(fr.getId(), fid, order));
            order++;
        }

        if (!toSave.isEmpty()) {
            refRepo.saveAll(toSave);
        }

        return new FileRefService.ReplaceAllResult(resultEntries);
    }

    @Override
    public void detachAll(FileOwnerType ownerType, String ownerId) {
        List<FileRef> refs = refRepo.findByOwnerTypeAndOwnerId(ownerType, ownerId);
        executeBulkDetach(refs);
    }

    @Override
    public void detachAll(FileOwnerType ownerType, String ownerId, FilePurpose purpose) {
        List<FileRef> refs = refRepo.findByOwnerTypeAndOwnerIdAndPurposeOrderBySortOrderAsc(
                ownerType, ownerId, purpose
        );
        executeBulkDetach(refs);
    }

    /**
     * 참조 삭제 및 참조 카운트 감소를 일괄 처리합니다.
     */
    private void executeBulkDetach(List<FileRef> refs) {
        if (refs.isEmpty()) return;

        Map<String, Integer> decCount = new HashMap<>();
        for (FileRef r : refs) {
            decCount.merge(r.getStoredFile().getId(), 1, Integer::sum);
        }

        refRepo.deleteAllInBatch(refs);

        // 🎯 더 이상 N+1 단건 루프는 없다. 파일별로 묶어서 한 방에 감산한다.
        for (Map.Entry<String, Integer> e : decCount.entrySet()) {
            fileRepo.decRefBulk(e.getKey(), e.getValue());
        }
    }
}