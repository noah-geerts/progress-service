package com.noahgeerts.progress.service;

import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.PerformedExercise.CreatePerformedExerciseDto;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExerciseResponseDto;
import com.noahgeerts.progress.domain.PerformedExercise.UpdatePerformedExerciseDto;
import com.noahgeerts.progress.domain.Session.Session;
import com.noahgeerts.progress.exceptions.BadRequestException;
import com.noahgeerts.progress.exceptions.ConflictException;
import com.noahgeerts.progress.exceptions.ResourceNotFoundException;
import com.noahgeerts.progress.exceptions.UnprocessableEntityException;
import com.noahgeerts.progress.repository.ExerciseRepository;
import com.noahgeerts.progress.repository.PerformedExerciseRepository;
import com.noahgeerts.progress.repository.SessionRepository;

@Service
public class PerformedExerciseService {
  private PerformedExerciseRepository peRepo;
  private ExerciseRepository exerciseRepo;
  private SessionRepository sessionRepo;
  private ModelMapper mapper;

  public PerformedExerciseService(PerformedExerciseRepository peRepo, SessionRepository sessionRepo,
      ExerciseRepository exerciseRepo,
      ModelMapper mapper) {
    this.peRepo = peRepo;
    this.sessionRepo = sessionRepo;
    this.exerciseRepo = exerciseRepo;
    this.mapper = mapper;
  }

  public PerformedExerciseResponseDto createPerformedExercise(String uid, CreatePerformedExerciseDto dto) {
    return createPerformedExercise(uid, UUID.randomUUID(), dto);
  }

  private PerformedExerciseResponseDto createPerformedExercise(String uid, UUID id, CreatePerformedExerciseDto dto) {
    // Check if it already exists
    Optional<PerformedExercise> existingPE = peRepo.findBySession_IdAndPositionAndUid(dto.getSessionId(),
        dto.getPosition(), uid);
    if (existingPE.isPresent())
      throw new ConflictException("PerformedExercise with the given session id and position already exist for this user");

    // Check that the provided exercise id and session id are valid
    Optional<Session> existingSession = sessionRepo.findByIdAndUid(dto.getSessionId(), uid);
    Optional<Exercise> existingExercise = exerciseRepo.findByIdAndUid(dto.getExerciseId(), uid);
    if (existingSession.isEmpty() || existingExercise.isEmpty())
      throw new UnprocessableEntityException("The session id or exercise id provided for the PerformedExercise is invalid");

    // Create the new PerformedExercise
    PerformedExercise newPE = PerformedExercise.builder().id(id).session(existingSession.get())
        .exercise(existingExercise.get()).position(dto.getPosition()).uid(uid).build();
    PerformedExercise created = peRepo.save(newPE);
    return mapper.map(created, PerformedExerciseResponseDto.class);
  }

  public ResponseEntity<PerformedExerciseResponseDto> createOrUpdatePerformedExercise(String uid, UUID id,
      CreatePerformedExerciseDto dto) {
    Optional<PerformedExercise> existing = peRepo.findByIdAndUid(id, uid);
    if (existing.isEmpty()) {
      if (peRepo.existsById(id))
        throw new ConflictException("The provided performed exercise id is unavailable");
      return ResponseEntity.status(HttpStatus.CREATED).body(createPerformedExercise(uid, id, dto));
    }

    PerformedExercise performedExercise = existing.get();
    if (!performedExercise.getSession().getId().equals(dto.getSessionId())
        || !performedExercise.getExercise().getId().equals(dto.getExerciseId()))
      throw new BadRequestException("sessionId and exerciseId may not be updated on a PerformedExercise after creation");

    Optional<PerformedExercise> positionOwner = peRepo.findBySession_IdAndPositionAndUid(dto.getSessionId(),
        dto.getPosition(), uid);
    if (positionOwner.isPresent() && !positionOwner.get().getId().equals(id))
      throw new ConflictException("PerformedExercise with the given session id and position already exist for this user");

    return ResponseEntity.ok(updateExistingPerformedExercise(performedExercise,
      performedExercise.getExercise(), dto.getPosition()));
  }

  public PerformedExerciseResponseDto updatePerformedExercise(String uid, UUID id, UpdatePerformedExerciseDto dto) {
    // Make sure it exists
    Optional<PerformedExercise> existingPE = peRepo.findByIdAndUid(id, uid);
    if (existingPE.isEmpty())
      throw new ResourceNotFoundException(
              "The given id does not correspond to a valid PerformedExercise for this user");

            // Check if the new exercise id is valid
    Optional<Exercise> newExercise = exerciseRepo.findByIdAndUid(dto.getExerciseId(), uid);
    if (newExercise.isEmpty())
      throw new UnprocessableEntityException("The provided exercise id does not correspond to a valid Exercise for this user");

    // Update the PerformedExercise
    PerformedExercise updated = existingPE.get();
    return updateExistingPerformedExercise(updated, newExercise.get(), updated.getPosition());
  }

  private PerformedExerciseResponseDto updateExistingPerformedExercise(PerformedExercise performedExercise,
      Exercise exercise, int position) {
    performedExercise.setExercise(exercise);
    performedExercise.setPosition(position);
    PerformedExercise result = peRepo.save(performedExercise);
    return mapper.map(result, PerformedExerciseResponseDto.class);
  }

  public void deletePerformedExercise(String uid, UUID id) {
    // Make sure it exists
    Optional<PerformedExercise> existingPE = peRepo.findByIdAndUid(id, uid);
    if (existingPE.isEmpty())
      throw new ResourceNotFoundException(
          "The given id does not correspond to a valid PerformedExercise for this user");

    // Delete it (and remove it from its parent session)
    PerformedExercise pe = existingPE.get();
    Session session = pe.getSession();
    session.getPerformedExercises().remove(pe);
    sessionRepo.save(session);
    peRepo.delete(pe);
  }
}
