package com.noahgeerts.progress.domain.PerformedSet;

import java.util.UUID;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
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
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(nullable = false)
  private int position;
  @Column(nullable = false)
  private int reps;
  @Column(nullable = false)
  private double weight;
  @Column(nullable = false)
  private String uid;

  @ManyToOne(optional = false)
  @JoinColumn(name = "performed_exercise_id", nullable = false)
  private PerformedExercise performedExercise;
}
