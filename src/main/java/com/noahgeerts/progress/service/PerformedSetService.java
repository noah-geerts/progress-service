package com.noahgeerts.progress.service;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedSet.CreatePerformedSetDto;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSetResponseDto;
import com.noahgeerts.progress.domain.PerformedSet.UpdatePerformedSetDto;
import com.noahgeerts.progress.exceptions.ConflictException;
import com.noahgeerts.progress.exceptions.ResourceNotFoundException;
import com.noahgeerts.progress.exceptions.UnprocessableEntityException;
import com.noahgeerts.progress.repository.PerformedExerciseRepository;
import com.noahgeerts.progress.repository.PerformedSetRepository;

@Service
public class PerformedSetService {
  private PerformedSetRepository setRepo;
  private PerformedExerciseRepository peRepo;
  private ModelMapper mapper;

  public PerformedSetService(PerformedSetRepository setRepo, PerformedExerciseRepository peRepo, ModelMapper mapper) {
    this.setRepo = setRepo;
    this.peRepo = peRepo;
    this.mapper = mapper;
  }

  /**
   * 
   * @param uid
   * @param dto
   * @return The newly created PerformedSet entity
   * @throws ConflictException            if a set already exists with the
   *                                      provided position
   *                                      and performed exercise for the given
   *                                      user
   * @throws UnprocessableEntityException if the provided peid does not correspond
   *                                      to a valid PerformedExercise
   */
  public PerformedSetResponseDto createPerformedSet(String uid, CreatePerformedSetDto dto) {
    // Check if the performedSet already exists
    Optional<PerformedSet> existing = setRepo.findByPerformedExercise_IdAndPositionAndUid(dto.getPerformedExerciseId(),
        dto.getPosition(), uid);
    if (!existing.isEmpty())
      throw new ConflictException("PerformedSet already exists with the provided peid and position for this user");

    // Check if the peid provided corresponds to a valid PerformedExercise owned by
    // this user
    Optional<PerformedExercise> existingPe = peRepo.findByIdAndUid(dto.getPerformedExerciseId(), uid);
    if (existingPe.isEmpty())
      throw new UnprocessableEntityException(
          "Provided peid does not correspond to a valid PerformedExercise for this user");

    // Create the new entity
    PerformedSet newSet = PerformedSet.builder().reps(dto.getReps()).weight(dto.getWeight()).position(dto.getPosition())
        .performedExercise(existingPe.get()).uid(uid).build();
    PerformedSet created = setRepo.save(newSet);
    return mapper.map(created, PerformedSetResponseDto.class);
  }

  /**
   * 
   * @param uid
   * @param dto
   * @return The updated PerformedSet entity
   * @throws ResourceNotFoundException if there is no PerformedSet with the given
   *                                   stid for the current user
   */
  public PerformedSetResponseDto updatePerformedSet(String uid, UUID stid, UpdatePerformedSetDto dto) {
    // Check if it exists
    Optional<PerformedSet> existing = setRepo.findByIdAndUid(stid, uid);
    if (existing.isEmpty())
      throw new ResourceNotFoundException("PerformedSet with the given stid does not exist for this user");

    // Update it
    PerformedSet oldSet = existing.get();
    oldSet.setReps(dto.getReps());
    oldSet.setWeight(dto.getWeight());
    PerformedSet newSet = setRepo.save(oldSet);
    return mapper.map(newSet, PerformedSetResponseDto.class);
  }

  /**
   * Deletes the PerformedSet by id if it belongs to the user
   * 
   * @param uid
   * @param dto
   * @throws ResourceNotFoundException if there is no PerformedSet with the given
   *                                   stid for the current user
   */
  public void deletePerformedSet(String uid, UUID stid) {
    // Check if it exists
    Optional<PerformedSet> existing = setRepo.findByIdAndUid(stid, uid);
    if (existing.isEmpty())
      throw new ResourceNotFoundException("PerformedSet with the given stid does not exist for this user");

    // Delete it (and remove it from its parent performed exercise)
    PerformedSet set = existing.get();
    PerformedExercise pe = set.getPerformedExercise();
    pe.getSets().remove(set);
    peRepo.save(pe);
    setRepo.delete(set);
  }

}
