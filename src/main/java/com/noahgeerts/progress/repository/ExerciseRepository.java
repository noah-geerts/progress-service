package com.noahgeerts.progress.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.noahgeerts.progress.domain.Exercise.Exercise;

public interface ExerciseRepository extends CrudRepository<Exercise, Long> {
  public List<Exercise> findAllByUidOrderByNameAsc(String uid);

  public Optional<Exercise> findByNameAndUid(String name, String uid);

  public Optional<Exercise> findByEidAndUid(Long eid, String uid);
}
