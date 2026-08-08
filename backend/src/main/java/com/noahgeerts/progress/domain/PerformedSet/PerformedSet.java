package com.noahgeerts.progress.domain.PerformedSet;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformedSet {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long stid;

  @Column(nullable = false)
  private int position;
  @Column(nullable = false)
  private int reps;
  @Column(nullable = false)
  private double weight;
  @Column(nullable = false)
  private String uid;

  @ManyToOne
  private PerformedExercise performedExercise;
}
