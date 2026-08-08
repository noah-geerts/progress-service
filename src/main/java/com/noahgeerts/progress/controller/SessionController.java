package com.noahgeerts.progress.controller;

import java.lang.annotation.Repeatable;
import java.time.LocalDate;
import java.util.List;

import org.springframework.cglib.core.Local;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noahgeerts.progress.domain.Session.SessionRequestDto;
import com.noahgeerts.progress.domain.Session.SessionResponseDto;
import com.noahgeerts.progress.service.SessionService;

@RestController
@RequestMapping("sessions")
public class SessionController {
  private SessionService sessionService;

  public SessionController(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @GetMapping("/monthly/{date}")
  public ResponseEntity<List<SessionResponseDto>> getMonthlySessions(@AuthenticationPrincipal Jwt jwt,
      @PathVariable LocalDate date) {
    List<SessionResponseDto> sessions = sessionService.getMonthlySessions(jwt.getSubject(), date);
    return ResponseEntity.ok(sessions);
  }

  @GetMapping("/{date}")
  public ResponseEntity<SessionResponseDto> getSessionByDate(@AuthenticationPrincipal Jwt jwt,
      @PathVariable LocalDate date) {
    SessionResponseDto session = sessionService.getSession(jwt.getSubject(), date);
    return ResponseEntity.ok(session);
  }

  @PostMapping("/{date}")
  public ResponseEntity<SessionResponseDto> createSession(@AuthenticationPrincipal Jwt jwt,
      @PathVariable LocalDate date, @Validated @RequestBody SessionRequestDto dto) {
    SessionResponseDto newSession = sessionService.createSession(jwt.getSubject(), date, dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(newSession);
  }

  @PatchMapping("/{date}")
  public ResponseEntity<SessionResponseDto> updateSession(@AuthenticationPrincipal Jwt jwt,
      @PathVariable LocalDate date, @Validated @RequestBody SessionRequestDto dto) {
    SessionResponseDto updatedSession = sessionService.updateSession(jwt.getSubject(), date, dto);
    return ResponseEntity.ok(updatedSession);
  }

  @DeleteMapping("/{date}")
  public ResponseEntity<SessionResponseDto> deleteSession(@AuthenticationPrincipal Jwt jwt,
      @PathVariable LocalDate date) {
    sessionService.deleteSession(jwt.getSubject(), date);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
