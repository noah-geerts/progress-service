package com.noahgeerts.progress.domain.PerformedSet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePerformedSetDto {
  @NotNull
  @Min(1) // sets have at least 1 rep
  private Integer reps;

  @NotNull
  @DecimalMin("0.0") // weight can be 0 but not less
  private Double weight;
}
