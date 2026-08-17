package com.noahgeerts.progress.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;

public interface PerformedExerciseRepository extends CrudRepository<PerformedExercise, UUID> {
  public Optional<PerformedExercise> findByIdAndUid(UUID id, String uid);

  public Optional<PerformedExercise> findBySession_IdAndPositionAndUid(UUID sessionId, int position, String uid);
}
