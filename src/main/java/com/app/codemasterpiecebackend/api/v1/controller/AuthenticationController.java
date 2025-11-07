package com.app.codemasterpiecebackend.api.v1.controller;

import com.app.codemasterpiecebackend.domain.dto.response.SuccessPayload;
import com.app.codemasterpiecebackend.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.support.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public SuccessPayload<AppUserDetails.AppUser> getAppUser(@AuthenticationPrincipal AppUserDetails userDetails) {
        if (userDetails == null || userDetails.getAppUser() == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "error.auth.unauthenticated");
        }

        return SuccessPayload.of(userDetails.getAppUser());
    }

    @GetMapping("/ping")
    public Map<String, Object> sessionTouch(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        long now = System.currentTimeMillis();
        long last = s.getLastAccessedTime();
        int maxSec = s.getMaxInactiveInterval();
        long expAt = last + (maxSec * 1000L);
        long expMs = Math.max(0, expAt - now);

        return Map.of(
                "ok", true,
                "expMs", expMs,
                "serverTime", now
        );
    }
}
