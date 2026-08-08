package com.noahgeerts.progress.domain.PerformedExercise;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePerformedExerciseDto {
  @NotNull
  private Long eid;
}
