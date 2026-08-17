package com.noahgeerts.progress.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.noahgeerts.progress.domain.Exercise.Exercise;

public interface ExerciseRepository extends CrudRepository<Exercise, UUID> {
  public List<Exercise> findAllByUidOrderByNameAsc(String uid);

  public Optional<Exercise> findByNameAndUid(String name, String uid);

  public Optional<Exercise> findByIdAndUid(UUID id, String uid);
}
