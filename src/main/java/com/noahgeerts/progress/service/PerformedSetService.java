package com.noahgeerts.progress.service;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedSet.CreatePerformedSetDto;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSetResponseDto;
import com.noahgeerts.progress.domain.PerformedSet.UpdatePerformedSetDto;
import com.noahgeerts.progress.exceptions.ConflictException;
import com.noahgeerts.progress.exceptions.ResourceNotFoundException;
import com.noahgeerts.progress.exceptions.UnprocessableEntityException;
import com.noahgeerts.progress.exceptions.BadRequestException;
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

  public PerformedSetResponseDto createPerformedSet(String uid, CreatePerformedSetDto dto) {
    return createPerformedSet(uid, UUID.randomUUID(), dto);
  }

  private PerformedSetResponseDto createPerformedSet(String uid, UUID id, CreatePerformedSetDto dto) {
    // Check if the performedSet already exists
    Optional<PerformedSet> existing = setRepo.findByPerformedExercise_IdAndPositionAndUid(dto.getPerformedExerciseId(),
        dto.getPosition(), uid);
    if (!existing.isEmpty())
      throw new ConflictException("PerformedSet already exists with the provided performed exercise id and position for this user");

    // Check if the performed exercise id corresponds to a valid PerformedExercise owned by
    // this user
    Optional<PerformedExercise> existingPe = peRepo.findByIdAndUid(dto.getPerformedExerciseId(), uid);
    if (existingPe.isEmpty())
      throw new UnprocessableEntityException(
          "Provided performed exercise id does not correspond to a valid PerformedExercise for this user");

    // Create the new entity
    PerformedSet newSet = PerformedSet.builder().id(id).reps(dto.getReps()).weight(dto.getWeight()).position(dto.getPosition())
        .performedExercise(existingPe.get()).uid(uid).build();
    PerformedSet created = setRepo.save(newSet);
    return mapper.map(created, PerformedSetResponseDto.class);
  }

  public PerformedSetResponseDto updatePerformedSet(String uid, UUID id, UpdatePerformedSetDto dto) {
    // Check if it exists
    Optional<PerformedSet> existing = setRepo.findByIdAndUid(id, uid);
    if (existing.isEmpty())
      throw new ResourceNotFoundException("PerformedSet with the given id does not exist for this user");

    return updateExistingPerformedSet(existing.get(), dto);
  }

  private PerformedSetResponseDto updateExistingPerformedSet(PerformedSet oldSet, UpdatePerformedSetDto dto) {
    oldSet.setReps(dto.getReps());
    oldSet.setWeight(dto.getWeight());
    PerformedSet newSet = setRepo.save(oldSet);
    return mapper.map(newSet, PerformedSetResponseDto.class);
  }

  public ResponseEntity<PerformedSetResponseDto> createOrUpdatePerformedSet(String uid, UUID id, CreatePerformedSetDto dto) {
    // Check if it exists
    Optional<PerformedSet> existing = setRepo.findByIdAndUid(id, uid);

    // If it doesn't exist try to create it
    if(existing.isEmpty()) {
      return ResponseEntity.status(HttpStatus.CREATED).body(createPerformedSet(uid, id, dto));
    }

    // If it exists, update it, but do not permit the performed exercise id or position to be updated
    if(!existing.get().getPerformedExercise().getId().equals(dto.getPerformedExerciseId()))
      throw new BadRequestException("performedExerciseId may not be updated on a PerformedSet after creation");
    if(existing.get().getPosition() != dto.getPosition().intValue())
      throw new BadRequestException("position may not be updated on a PerformedSet after creation");

    UpdatePerformedSetDto updateDto = mapper.map(dto, UpdatePerformedSetDto.class);
    return ResponseEntity.ok(updateExistingPerformedSet(existing.get(), updateDto));
  }

  public void deletePerformedSet(String uid, UUID id) {
    // Check if it exists
    Optional<PerformedSet> existing = setRepo.findByIdAndUid(id, uid);
    if (existing.isEmpty())
      throw new ResourceNotFoundException("PerformedSet with the given id does not exist for this user");

    // Delete it (and remove it from its parent performed exercise)
    PerformedSet set = existing.get();
    PerformedExercise pe = set.getPerformedExercise();
    pe.getSets().remove(set);
    peRepo.save(pe);
    setRepo.delete(set);
  }

}
