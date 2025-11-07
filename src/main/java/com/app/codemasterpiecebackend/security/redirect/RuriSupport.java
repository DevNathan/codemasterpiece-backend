package com.app.codemasterpiecebackend.security.redirect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class RuriSupport {
    private RuriSupport() {
    }

    public static final String SESSION_KEY = "RURI";

    public static String safeRuri(String r) {
        if (r == null || r.isBlank()) return null;
        if (r.contains("\r") || r.contains("\n")) return null;
        if (r.startsWith("http://") || r.startsWith("https://")) return null; // open redirect block
        if (!r.startsWith("/")) return null;
        if (r.startsWith("//")) return null;
        return r;
    }

    public static void saveToSession(HttpServletRequest req, String ruri) {
        String s = safeRuri(ruri);
        if (s == null) return;
        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_KEY, s);
    }

    public static String loadOnce(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        Object v = session.getAttribute(SESSION_KEY);
        if (!(v instanceof String s)) return null;
        session.removeAttribute(SESSION_KEY);
        return safeRuri(s);
    }
}
