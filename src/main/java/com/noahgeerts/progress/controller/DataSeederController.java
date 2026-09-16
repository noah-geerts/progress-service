package com.noahgeerts.progress.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noahgeerts.progress.DataSeeder;

@RestController
@RequestMapping("/dev/seed")
@Profile("dev & !prod")
public class DataSeederController {
  private final DataSeeder dataSeeder;

  public DataSeederController(DataSeeder dataSeeder) {
    this.dataSeeder = dataSeeder;
  }

  @PostMapping
  public ResponseEntity<Void> seed() throws Exception {
    dataSeeder.run();
    return ResponseEntity.noContent().build();
  }
}