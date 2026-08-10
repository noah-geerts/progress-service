package com.noahgeerts.progress.domain.Exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Exercise {
  @Id  //TODO: migrate to UUID
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long eid;

  @Column(nullable = false)  //TODO: add max length 50
  private String name;
  @Column(nullable = false)
  private String uid;
}
