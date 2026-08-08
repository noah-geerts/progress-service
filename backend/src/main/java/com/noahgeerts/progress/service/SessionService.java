package com.noahgeerts.progress.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;
import com.noahgeerts.progress.domain.Session.Session;
import com.noahgeerts.progress.domain.Session.SessionRequestDto;
import com.noahgeerts.progress.domain.Session.SessionResponseDto;
import com.noahgeerts.progress.exceptions.ConflictException;
import com.noahgeerts.progress.exceptions.ResourceNotFoundException;
import com.noahgeerts.progress.repository.SessionRepository;

@Service
public class SessionService {
  private SessionRepository sessionRepo;
  private ModelMapper mapper;

  public SessionService(SessionRepository sessionRepo, ModelMapper mapper) {
    this.sessionRepo = sessionRepo;
    this.mapper = mapper;
  }

  /**
   * 
   * @param uid
   * @param date
   * @return The requested Session
   * @throws ResourceNotFoundException if there is no session on that date for the
   *                                   given user
   */
  public SessionResponseDto getSession(String uid, LocalDate date) {
    // Check that session exists
    Optional<Session> existing = sessionRepo.findByDateAndUid(date, uid);
    if (existing.isEmpty())
      throw new ResourceNotFoundException("There is no session on the given date for this user");

    // Sort the performed exercises and performed sets by position increasing
    Session session = existing.get();
    if (session.getPerformedExercises() != null) {
      // Sort performed exercises by position
      session.getPerformedExercises().sort(Comparator.comparingInt(PerformedExercise::getPosition));

      // Sort sets within each performed exercise by position
      session.getPerformedExercises().forEach(pe -> {
        if (pe.getSets() != null)
          pe.getSets().sort(Comparator.comparingInt(PerformedSet::getPosition));
      });
    }

    // Return the session
    return mapper.map(session, SessionResponseDto.class);
  }

  /**
   * 
   * @param uid
   * @param date
   * @return A List of the sessions for the desired month (gets sessions for all
   *         days in the month that the provided date falls in)
   */
  public List<SessionResponseDto> getMonthlySessions(String uid, LocalDate date) {
    // Get the first day of the month
    LocalDate firstDayOfMonth = date.withDayOfMonth(1);
    int monthOfYear = date.getMonthValue();

    // Iterate through all days of the month
    ArrayList<SessionResponseDto> output = new ArrayList<>();
    LocalDate currentDate = firstDayOfMonth;

    while (currentDate.getMonthValue() == monthOfYear) {
      try {
        SessionResponseDto session = getSession(uid, currentDate);
        output.add(session);
      } catch (ResourceNotFoundException e) {
        // Session doesn't exist for this date, continue to next day
      }

      // Move to next day (reassign because LocalDate is immutable)
      currentDate = currentDate.plusDays(1);
    }

    return output;
  }

  /**
   * 
   * @param uid
   * @param date
   * @param dto
   * @return
   * @throws ConflictException if there is already a session on the given date
   */
  public SessionResponseDto createSession(String uid, LocalDate date, SessionRequestDto dto) {
    // Check if the session already exists
    Optional<Session> session = sessionRepo.findByDateAndUid(date, uid);
    if (session.isPresent())
      throw new ConflictException("There is already a session on the given date for this user");

    // Create the session
    Session newSession = Session.builder().date(date).name(dto.getName()).uid(uid).build();
    Session created = sessionRepo.save(newSession);
    return mapper.map(created, SessionResponseDto.class);
  }

  public SessionResponseDto updateSession(String uid, LocalDate date, SessionRequestDto dto) {
    // Check that session exists
    Optional<Session> session = sessionRepo.findByDateAndUid(date, uid);
    if (session.isEmpty())
      throw new ResourceNotFoundException("There is no session on the given date for this user");

    // Update the session
    Session updated = session.get();
    updated.setName(dto.getName());
    Session saved = sessionRepo.save(updated);
    return mapper.map(saved, SessionResponseDto.class);
  }

  public void deleteSession(String uid, LocalDate date) {
    // Check that session exists
    Optional<Session> session = sessionRepo.findByDateAndUid(date, uid);
    if (session.isEmpty())
      throw new ResourceNotFoundException("There is no session on the given date for this user");

    // Delete the session
    sessionRepo.delete(session.get());
  }
}
