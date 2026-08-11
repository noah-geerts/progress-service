package com.noahgeerts.progress.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noahgeerts.progress.domain.PerformedExercise.CreatePerformedExerciseDto;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExerciseResponseDto;
import com.noahgeerts.progress.domain.PerformedExercise.UpdatePerformedExerciseDto;
import com.noahgeerts.progress.service.PerformedExerciseService;

@RestController
@RequestMapping("/performed-exercises")
public class PerformedExerciseController {
  private PerformedExerciseService peService;

  public PerformedExerciseController(PerformedExerciseService peService) {
    this.peService = peService;
  }

  @PostMapping
  public ResponseEntity<PerformedExerciseResponseDto> createPerformedExercise(@AuthenticationPrincipal Jwt jwt,
      @Validated @RequestBody CreatePerformedExerciseDto dto) {
    PerformedExerciseResponseDto result = peService.createPerformedExercise(jwt.getSubject(), dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PatchMapping("/{peid}")
  public ResponseEntity<PerformedExerciseResponseDto> updatePerformedExercise(@AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID peid,
      @Validated @RequestBody UpdatePerformedExerciseDto dto) {
    PerformedExerciseResponseDto result = peService.updatePerformedExercise(jwt.getSubject(), peid, dto);
    return ResponseEntity.ok(result);
  }

  @DeleteMapping("/{peid}")
  public ResponseEntity<Void> deletePerformedExercise(@AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID peid) {
    peService.deletePerformedExercise(jwt.getSubject(), peid);
    return ResponseEntity.noContent().build();
  }
}
