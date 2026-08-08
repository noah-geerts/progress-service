package com.noahgeerts.progress.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;

public interface PerformedSetRepository extends CrudRepository<PerformedSet, Long> {
  public Optional<PerformedSet> findByPerformedExercise_PeidAndPositionAndUid(Long peid, int position, String uid);

  public Optional<PerformedSet> findByStidAndUid(Long stid, String uid);
}
