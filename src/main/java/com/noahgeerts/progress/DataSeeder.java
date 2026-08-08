package com.noahgeerts.progress;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;
import com.noahgeerts.progress.domain.Session.Session;
import com.noahgeerts.progress.repository.ExerciseRepository;
import com.noahgeerts.progress.repository.SessionRepository;

@Component
public class DataSeeder {

  private static final String TEST_UID = "auth0|68d5beb4de9727f2ce19a4ca"; // Test UID

  @Autowired
  private ExerciseRepository exerciseRepository;

  @Autowired
  private SessionRepository sessionRepository;

  public void run() throws Exception {
    // Seed exercises first
    seedExercises();

    // Seed sessions for current week
    seedCurrentWeekSessions();
  }

  private void seedExercises() {
    String[] exerciseNames = {
        "Bench Press",
        "Squat",
        "Deadlift",
        "Overhead Press",
        "Pull-ups"
    };

    for (String name : exerciseNames) {
      if (exerciseRepository.findByNameAndUid(name, TEST_UID).isEmpty()) {
        Exercise exercise = Exercise.builder()
            .name(name)
            .uid(TEST_UID)
            .build();
        exerciseRepository.save(exercise);
      }
    }
  }

  private void seedCurrentWeekSessions() {
    LocalDate today = LocalDate.now();
    LocalDate monday = today.with(DayOfWeek.MONDAY);

    // Session data: [dayOffset, sessionName, exerciseName, sets[weight,reps]]
    Object[][] sessionData = {
        { 0, "Push Day", "Bench Press", new int[][] { { 225, 8 }, { 235, 6 }, { 245, 4 } } },
        { 2, "Pull Day", "Deadlift", new int[][] { { 315, 5 }, { 335, 3 }, { 365, 1 } } },
        { 3, "Leg Day", "Squat", new int[][] { { 275, 10 }, { 295, 8 }, { 315, 6 } } },
        { 5, "Upper Body", "Overhead Press", new int[][] { { 135, 10 }, { 145, 8 }, { 155, 6 } } },
        { 6, "Recovery", "Pull-ups", new int[][] { { 0, 12 }, { 25, 10 }, { 35, 8 } } }
    };

    for (Object[] data : sessionData) {
      int dayOffset = (int) data[0];
      String sessionName = (String) data[1];
      String exerciseName = (String) data[2];
      int[][] sets = (int[][]) data[3];

      LocalDate sessionDate = monday.plusDays(dayOffset);

      // Skip if session already exists for this date
      if (sessionRepository.findByDateAndUid(sessionDate, TEST_UID).isPresent()) {
        continue;
      }

      // Find the exercise
      Exercise exercise = exerciseRepository.findByNameAndUid(exerciseName, TEST_UID).orElse(null);
      if (exercise == null)
        continue;

      // Create performed sets
      ArrayList<PerformedSet> performedSets = new ArrayList<>();
      for (int i = 0; i < sets.length; i++) {
        PerformedSet set = PerformedSet.builder()
            .position(i)
            .weight(sets[i][0])
            .reps(sets[i][1])
            .uid(TEST_UID)
            .build();
        performedSets.add(set);
      }

      // Create performed exercise
      PerformedExercise performedExercise = PerformedExercise.builder()
          .position(0)
          .exercise(exercise)
          .uid(TEST_UID)
          .sets(performedSets)
          .build();

      // Set bidirectional relationships
      for (PerformedSet set : performedSets) {
        set.setPerformedExercise(performedExercise);
      }

      // Create session
      Session session = Session.builder()
          .date(sessionDate)
          .name(sessionName)
          .uid(TEST_UID)
          .performedExercises(new ArrayList<>())
          .build();

      session.getPerformedExercises().add(performedExercise);
      performedExercise.setSession(session);

      sessionRepository.save(session);
    }
  }
}
