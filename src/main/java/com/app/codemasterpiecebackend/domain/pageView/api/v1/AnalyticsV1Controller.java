package com.app.codemasterpiecebackend.domain.pageView.api.v1;

import com.app.codemasterpiecebackend.domain.pageView.dto.PageViewEvent;
import com.app.codemasterpiecebackend.domain.pageView.application.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/collect")
public class AnalyticsV1Controller {
  private final AnalyticsService service;

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void collect(@RequestBody PageViewEvent event, HttpServletRequest req) {
    if (event == null || !"page_view".equalsIgnoreCase(event.type())) return;
    service.ingest(event, req);
  }
}
