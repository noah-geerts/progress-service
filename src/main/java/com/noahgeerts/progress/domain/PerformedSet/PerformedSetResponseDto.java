package com.noahgeerts.progress.domain.PerformedSet;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PerformedSetResponseDto {
  private UUID id;

  private Integer position;
  private Integer reps;
  private Double weight;
}
