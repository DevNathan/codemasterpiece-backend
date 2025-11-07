package com.app.codemasterpiecebackend.domain.dto.analytics;

import java.util.Map;

public record PageViewEvent(
  String type,      // "page_view"
  Long ts,          // epoch ms (클라 시간)
  String cid,
  String sid,
  String url,
  String ref,
  String title,
  String lang,
  Map<String, String> utm,   // utm_source/medium/campaign/term/content
  Viewport vp,
  String ua                  // userAgent (서버에서 파싱)
) {
  public record Viewport(Integer w, Integer h) {}
}
