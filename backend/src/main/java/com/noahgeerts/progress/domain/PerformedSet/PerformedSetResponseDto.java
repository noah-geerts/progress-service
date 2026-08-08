package com.noahgeerts.progress.domain.PerformedSet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformedSetResponseDto {
  private Long stid;

  private Integer position;
  private Integer reps;
  private Double weight;
}
