package com.noahgeerts.progress.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;

public interface PerformedSetRepository extends CrudRepository<PerformedSet, UUID> {
  public Optional<PerformedSet> findByPerformedExercise_IdAndPositionAndUid(UUID performedExerciseId, int position,
      String uid);

  public Optional<PerformedSet> findByIdAndUid(UUID id, String uid);
}
