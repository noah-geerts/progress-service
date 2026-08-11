package com.noahgeerts.progress.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.noahgeerts.progress.domain.Session.Session;

public interface SessionRepository extends CrudRepository<Session, UUID> {
  public Optional<Session> findByIdAndUid(UUID id, String uid);

  public Optional<Session> findByDateAndUid(LocalDate date, String uid);
}
