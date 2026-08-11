package com.noahgeerts.progress.domain.Session;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(nullable = false)
  private LocalDate date;
  @Column(nullable = false) //TODO: add max length 50
  private String name;
  @Column(nullable = false)
  private String uid;

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @ToString.Exclude
  private List<PerformedExercise> performedExercises;
}
