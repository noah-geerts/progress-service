package com.noahgeerts.progress.domain.PerformedExercise;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePerformedExerciseDto {
  @NotNull
  private UUID exerciseId;
  
  @NotNull
  private UUID sessionId;

  @NotNull
  @Min(0) // can't have negative positions. 0 indexed
  private Integer position;
}
