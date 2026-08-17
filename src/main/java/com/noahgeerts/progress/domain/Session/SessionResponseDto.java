package com.noahgeerts.progress.domain.Session;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExerciseResponseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponseDto {
  private UUID id;

  private LocalDate date;
  private String name;

  private List<PerformedExerciseResponseDto> performedExercises;
}
