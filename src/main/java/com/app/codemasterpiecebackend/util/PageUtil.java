package com.app.codemasterpiecebackend.util;

import org.springframework.data.domain.Page;

import java.util.HashMap;
import java.util.Map;

public class PageUtil {
    public static <T> Map<String, Object> toResponseMap(Page<T> page) {
        Map<String, Object> res = new HashMap<>(2);

        // 1) 컨텐츠는 최상위
        res.put("content", page.getContent());

        // 2) 페이지 메타는 pagination 하위로
        Map<String, Object> pagination = new HashMap<>(10);
        int currentPage = page.getNumber() + 1;
        int pageSize = page.getSize();
        int totalPages = page.getTotalPages();

        pagination.put("currentPage", currentPage);
        pagination.put("pageSize", pageSize);
        pagination.put("totalPages", totalPages);
        pagination.put("totalElements", page.getTotalElements());
        pagination.put("first", page.isFirst());
        pagination.put("last", page.isLast());
        pagination.put("hasNext", page.hasNext());
        pagination.put("hasPrevious", page.hasPrevious());

        // --- 블럭 계산 ---
        int blockSize = 5;
        int start = ((currentPage - 1) / blockSize) * blockSize + 1;
        int end = Math.min(start + blockSize - 1, totalPages);

        pagination.put("pageStart", start);
        pagination.put("pageEnd", end);
        pagination.put("blockSize", blockSize);
        // --------------------

        res.put("pagination", pagination);
        return res;
    }
}
