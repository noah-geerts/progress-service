package com.noahgeerts.progress.controller;

import com.noahgeerts.progress.domain.Exercise.ExerciseRequestDto;
import com.noahgeerts.progress.domain.Exercise.ExerciseResponseDto;
import com.noahgeerts.progress.service.ExerciseService;

import java.util.List;
import java.util.UUID;

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

@RestController
@RequestMapping("/exercises")
public class ExerciseController {
  private ExerciseService exerciseService;

  public ExerciseController(ExerciseService exerciseService) {
    this.exerciseService = exerciseService;
  }

  @GetMapping()
  public ResponseEntity<List<ExerciseResponseDto>> getAllExercises(@AuthenticationPrincipal Jwt jwt) {
    List<ExerciseResponseDto> exercises = exerciseService.getAllExercises(jwt.getSubject());
    return ResponseEntity.ok(exercises);
  }

  @PostMapping()
  public ResponseEntity<ExerciseResponseDto> createExercise(@AuthenticationPrincipal Jwt jwt,
      @Validated @RequestBody ExerciseRequestDto dto) {
    ExerciseResponseDto newExercise = exerciseService.createExercise(jwt.getSubject(), dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(newExercise);
  }

  @PatchMapping("/{eid}")
  public ResponseEntity<ExerciseResponseDto> updateExercise(@AuthenticationPrincipal Jwt jwt,
      @Validated @RequestBody ExerciseRequestDto dto, @PathVariable UUID eid) {
    ExerciseResponseDto updatedExercise = exerciseService.updateExercise(jwt.getSubject(), dto, eid);
    return ResponseEntity.ok(updatedExercise);
  }

  @DeleteMapping("/{eid}")
  public ResponseEntity<Void> deleteExercise(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eid) {
    exerciseService.deleteExercise(jwt.getSubject(), eid);
    return ResponseEntity.noContent().build();
  }
}
