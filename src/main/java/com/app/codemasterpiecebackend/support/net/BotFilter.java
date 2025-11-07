package com.app.codemasterpiecebackend.support.net;

import java.util.Set;

public final class BotFilter {
  private static final Set<String> KW = Set.of(
    "bot","crawl","spider","snippet","preview","headless","puppeteer"
  );
  public static boolean isBot(String ua) {
    if (ua == null) return false;
    String s = ua.toLowerCase();
    for (String k: KW) if (s.contains(k)) return true;
    return false;
  }
}
