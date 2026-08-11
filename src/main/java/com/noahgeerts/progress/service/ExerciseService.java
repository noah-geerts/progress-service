package com.noahgeerts.progress.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.Exercise.ExerciseRequestDto;
import com.noahgeerts.progress.domain.Exercise.ExerciseResponseDto;
import com.noahgeerts.progress.repository.ExerciseRepository;
import com.noahgeerts.progress.exceptions.*;

@Service
public class ExerciseService {
  private ExerciseRepository exerciseRepo;

  private ModelMapper mapper;

  public ExerciseService(ExerciseRepository exerciseRepo, ModelMapper mapper) {
    this.exerciseRepo = exerciseRepo;
    this.mapper = mapper;
  }

  /**
   * @param uid
   * @return all exercises for the given user, sorted alphabetically
   */
  public List<ExerciseResponseDto> getAllExercises(String uid) {
    Iterable<Exercise> result = exerciseRepo.findAllByUidOrderByNameAsc(uid);
    return StreamSupport.stream(result.spliterator(), false)
        .map(exercise -> mapper.map(exercise, ExerciseResponseDto.class)).toList();
  }

  /**
   * 
   * @param uid
   * @param dto
   * @return the exercise just created under the given user
   * @throws ConflictException if an exercise already exists with the name
   *                           provided in the dto
   */
  public ExerciseResponseDto createExercise(String uid, ExerciseRequestDto dto) {
    // Check if an exercise with that name already exists
    Optional<Exercise> existing = exerciseRepo.findByNameAndUid(dto.getName(), uid);
    if (!existing.isEmpty())
      throw new ConflictException("Exercise already exists with the provided name for this user");

    // If not create one
    Exercise newExercise = Exercise.builder().name(dto.getName()).uid(uid).build();
    Exercise created = exerciseRepo.save(newExercise);
    return mapper.map(created, ExerciseResponseDto.class);
  }

  /**
   * Updates the name of the exercise
   * 
   * @param eid
   * @throws ResourceNotFoundException if the exercise does not exist for the
   *                                   given user
   * @throws ConflictException         if an exercise already exists with the name
   *                                   provided in the dto
   */
  public ExerciseResponseDto updateExercise(String uid, ExerciseRequestDto dto, UUID eid) {
    // Check if the exercise exists by id
    Optional<Exercise> existingById = exerciseRepo.findByIdAndUid(eid, uid);
    if (existingById.isEmpty())
      throw new ResourceNotFoundException("Exercise with provided eid does not exist for this user");

    // Check if there is already another exercise with the desired name
    Optional<Exercise> existingByName = exerciseRepo.findByNameAndUid(dto.getName(), uid);
    if (!existingByName.isEmpty())
      throw new ConflictException("Exercise with updated name already exists for this user");

    // Update exercise
    Exercise oldExercise = existingById.get();
    oldExercise.setName(dto.getName());
    Exercise updatedExercise = exerciseRepo.save(oldExercise);
    return mapper.map(updatedExercise, ExerciseResponseDto.class);
  }

  /**
   * Deletes the exercise
   * 
   * @param eid
   * @throws ResourceNotFoundException    if the exercise does not exist for the
   *                                      given user
   * @throws UnprocessableEntityException if the exercise is being referenced in
   *                                      Performed Exercises
   */
  public void deleteExercise(String uid, UUID eid) {
    // Check if the exercise exists by id
    Optional<Exercise> existingById = exerciseRepo.findByIdAndUid(eid, uid);
    if (existingById.isEmpty())
      throw new ResourceNotFoundException("Exercise with provided eid does not exist for this user");

    // Delete it
    try {
      exerciseRepo.delete(existingById.get());
    } catch (DataIntegrityViolationException e) {
      throw new UnprocessableEntityException(e.getMessage());
    }
  }

}
