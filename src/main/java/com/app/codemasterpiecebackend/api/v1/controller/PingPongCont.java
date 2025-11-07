package com.app.codemasterpiecebackend.api.v1.controller;

import com.app.codemasterpiecebackend.support.web.SkipWrap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SkipWrap
public class PingPongCont {
    @GetMapping("/ping")
    public String ping() {
        return "pong!";
    }
}
