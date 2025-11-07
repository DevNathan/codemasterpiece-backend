package com.app.codemasterpiecebackend.api.v1.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/i18n")
@RequiredArgsConstructor
public class I18nController {
    private final MessageSource messageSource;

    @GetMapping("/message")
    public Map<String, String> getMessage(
            @RequestParam String code,
            @RequestParam(required = false) List<String> args,
            @RequestParam(required = false, defaultValue = "ko") String lang
    ) {
        Locale locale = Locale.forLanguageTag(lang);
        String msg = messageSource.getMessage(
                code,
                (args != null ? args.toArray() : null),
                code,
                locale
        );
        return Map.of("message", msg);
    }
}

