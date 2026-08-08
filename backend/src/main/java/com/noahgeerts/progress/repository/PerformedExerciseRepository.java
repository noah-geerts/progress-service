package com.noahgeerts.progress.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;

public interface PerformedExerciseRepository extends CrudRepository<PerformedExercise, Long> {
  public Optional<PerformedExercise> findByPeidAndUid(Long peid, String uid);

  public Optional<PerformedExercise> findBySession_SsidAndPositionAndUid(Long ssid, int position, String uid);
}
