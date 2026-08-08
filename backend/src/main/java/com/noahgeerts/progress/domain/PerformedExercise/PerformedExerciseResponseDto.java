package com.noahgeerts.progress.domain.PerformedExercise;

import java.util.List;

import com.noahgeerts.progress.domain.Exercise.ExerciseResponseDto;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSetResponseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformedExerciseResponseDto {
  private Long peid;

  private Integer position;
  private ExerciseResponseDto exercise;
  private List<PerformedSetResponseDto> sets;
}
