package com.noahgeerts.progress.domain.PerformedExercise;

import java.util.List;

import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.Session.Session;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class PerformedExercise {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long peid;

  @Column(nullable = false)
  private int position;

  @ManyToOne
  private Session session;

  @Column(nullable = false)
  private String uid;

  @ManyToOne(fetch = FetchType.EAGER)
  private Exercise exercise;

  @OneToMany(mappedBy = "performedExercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @ToString.Exclude
  private List<PerformedSet> sets;
}
