package com.noahgeerts.progress.domain.PerformedExercise;

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
  private Long eid;
  
  @NotNull
  private Long ssid;

  @NotNull
  @Min(0) // can't have negative positions. 0 indexed
  private Integer position;
}
