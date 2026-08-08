package com.noahgeerts.progress.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.noahgeerts.progress.domain.Session.Session;

public interface SessionRepository extends CrudRepository<Session, Long> {
  public Optional<Session> findBySsidAndUid(Long ssid, String uid);

  public Optional<Session> findByDateAndUid(LocalDate date, String uid);
}
