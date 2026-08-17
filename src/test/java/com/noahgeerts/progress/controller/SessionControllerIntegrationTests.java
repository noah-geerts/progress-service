package com.noahgeerts.progress.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;
import com.noahgeerts.progress.domain.Session.Session;
import com.noahgeerts.progress.domain.Session.SessionRequestDto;
import com.noahgeerts.progress.repository.ExerciseRepository;
import com.noahgeerts.progress.repository.PerformedExerciseRepository;
import com.noahgeerts.progress.repository.PerformedSetRepository;
import com.noahgeerts.progress.repository.SessionRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class SessionControllerIntegrationTests {
        @Autowired
        private SessionRepository sessionRepo;
        @Autowired
        private PerformedExerciseRepository peRepo;
        @Autowired
        private ExerciseRepository exerciseRepo;
        @Autowired
        private PerformedSetRepository setRepo;

        @Autowired
        private ObjectMapper objectMapper;
        @Autowired
        private MockMvc mockMvc;

        private static final String TEST_UID = "test_user";

        private List<Exercise> seededExercises;
        private List<Session> seededSessions;
        private List<PerformedExercise> seededPEs;
        private List<PerformedSet> seededSets;

        @BeforeEach
        void setup() {
                // Seed exercises
                seededExercises = List.of(Exercise.builder().name("Bench Press").uid(TEST_UID).build(),
                                Exercise.builder().name("Dumbell Press").uid(TEST_UID).build(),
                                Exercise.builder().name("Squat").uid(TEST_UID).build());
                exerciseRepo.saveAll(seededExercises);

                // Seed sessions
                seededSessions = List.of(
                                // Chest Day on jan 1 2025
                                Session.builder().name("Chest Day").uid(TEST_UID).date(LocalDate.of(2025, 1, 1))
                                                .build(),
                                // Leg Day on jan 2 2025
                                Session.builder().name("Leg Day").uid(TEST_UID).date(LocalDate.of(2025, 1, 2)).build());
                sessionRepo.saveAll(seededSessions);

                // Seed PerformedExercises
                seededPEs = List.of(
                                // Bench Press on Chest Day
                                PerformedExercise.builder().exercise(seededExercises.get(0))
                                                .session(seededSessions.get(0))
                                                .uid(TEST_UID).position(2).build(),
                                // Dumbell Press on Chest Day
                                PerformedExercise.builder().exercise(seededExercises.get(1))
                                                .session(seededSessions.get(0))
                                                .uid(TEST_UID).position(1).build(),
                                // Squat on Leg Day
                                PerformedExercise.builder().exercise(seededExercises.get(2))
                                                .session(seededSessions.get(1))
                                                .uid(TEST_UID).position(1).build());
                peRepo.saveAll(seededPEs);

                // Seed sets
                seededSets = List
                                .of(
                                                // Bench 225x5
                                                PerformedSet.builder().reps(5).weight(225.0)
                                                                .performedExercise(seededPEs.get(0)).uid(TEST_UID)
                                                                .position(1)
                                                                .build(),
                                                // Bench 220x4
                                                PerformedSet.builder().reps(4).weight(220.0)
                                                                .performedExercise(seededPEs.get(0)).position(2)
                                                                .uid(TEST_UID)
                                                                .build(),
                                                // Dumbell Press 60sx12
                                                PerformedSet.builder().reps(12).weight(60.0)
                                                                .performedExercise(seededPEs.get(1)).position(1)
                                                                .uid(TEST_UID)
                                                                .build(),
                                                // Squat 315x3
                                                PerformedSet.builder().reps(3).weight(315.0)
                                                                .performedExercise(seededPEs.get(2)).position(2)
                                                                .uid(TEST_UID)
                                                                .build(),
                                                // Squat 315x2
                                                PerformedSet.builder().reps(2).weight(315.0)
                                                                .performedExercise(seededPEs.get(2)).position(1)
                                                                .uid(TEST_UID)
                                                                .build());

                setRepo.saveAll(seededSets);
        }

        @AfterEach
        void teardown() {
                // Clear all repositories
                sessionRepo.deleteAll();
                peRepo.deleteAll();
                exerciseRepo.deleteAll();
                setRepo.deleteAll();
        }

        @Nested
        class GetSession {
                @Test
                void shouldReturnCorrectChestDayWorkout_WhenGettingChestDay() throws Exception {
                        // Act & Assert
                        mockMvc.perform(get("/sessions/2025-01-01")
                                        .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.name").value("Chest Day"))
                                        .andExpect(jsonPath("$.date").value("2025-01-01"))

                                        // Should have 2 performed exercises (Bench Press and Dumbell Press)
                                        .andExpect(jsonPath("$.performedExercises", hasSize(2)))

                                        // Check second performed exercise (Dumbell Press)
                                        .andExpect(jsonPath("$.performedExercises[0].exercise.name")
                                                        .value("Dumbell Press"))
                                        .andExpect(jsonPath("$.performedExercises[0].sets", hasSize(1))) // 1 dumbell
                                                                                                         // press set
                                        .andExpect(jsonPath("$.performedExercises[0].sets[0].weight").value(60.0))
                                        .andExpect(jsonPath("$.performedExercises[0].sets[0].reps").value(12))

                                        // Check first performed exercise (Bench Press)
                                        .andExpect(jsonPath("$.performedExercises[1].exercise.name")
                                                        .value("Bench Press"))
                                        .andExpect(jsonPath("$.performedExercises[1].sets", hasSize(2))) // 2 bench
                                                                                                         // press sets
                                        .andExpect(jsonPath("$.performedExercises[1].sets[0].weight").value(225.0))
                                        .andExpect(jsonPath("$.performedExercises[1].sets[0].reps").value(5))
                                        .andExpect(jsonPath("$.performedExercises[1].sets[1].weight").value(220.0))
                                        .andExpect(jsonPath("$.performedExercises[1].sets[1].reps").value(4));

                }

                @Test
                void shouldReturnCorrectLegDayWorkout_WhenGettingLegDay() throws Exception {
                        // Act & Assert
                        mockMvc.perform(get("/sessions/2025-01-02") // Fixed: Leg Day should be on Jan 2
                                        .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.name").value("Leg Day"))
                                        .andExpect(jsonPath("$.date").value("2025-01-02"))

                                        // Should have 1 performed exercise (Squat)
                                        .andExpect(jsonPath("$.performedExercises", hasSize(1)))
                                        .andExpect(jsonPath("$.performedExercises[0].exercise.name").value("Squat"))
                                        .andExpect(jsonPath("$.performedExercises[0].sets", hasSize(2))) // 2 squat sets
                                        .andExpect(jsonPath("$.performedExercises[0].sets[0].weight").value(315.0))
                                        .andExpect(jsonPath("$.performedExercises[0].sets[0].reps").value(2))
                                        .andExpect(jsonPath("$.performedExercises[0].sets[1].weight").value(315.0))
                                        .andExpect(jsonPath("$.performedExercises[0].sets[1].reps").value(3));
                }

                @Test
                void shouldReturnNotFound_WhenSessionDoesNotExist() throws Exception {
                        mockMvc.perform(get("/sessions/2025-12-25")
                                        .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void shouldReturnUnauthorized_WhenNoAuthToken() throws Exception {
                        mockMvc.perform(get("/sessions/2025-01-01"))
                                        .andExpect(status().isUnauthorized());
                }
        }

        @Nested
        class GetMonthlySessions {
                @Test
                void shouldReturnBothSessionsForJan2025_WhenGettingMonthlySessions() throws Exception {
                        mockMvc.perform(get("/sessions/monthly/2025-01-02")
                                        .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))).andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(2)))
                                        .andExpect(jsonPath("$[0].name").value("Chest Day"))
                                        .andExpect(jsonPath("$[1].name").value("Leg Day"));
                }
        }

        @Nested
        class CreateSession {
                @Test
                void shouldCreateSession_WhenSessionDoesNotExist() throws Exception {
                        // Arrange
                        String requestBody = objectMapper
                                        .writeValueAsString(
                                                        SessionRequestDto.builder().name("New Chest Session").build());

                        // Act & Assert (don't fetch for 2025 jan 1 or 2)
                        mockMvc
                                        .perform(post("/sessions/2025-01-03")
                                                        .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                                        .contentType("application/json").content(requestBody))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.date").value("2025-01-03"))
                                        .andExpect(jsonPath("$.name").value("New Chest Session"));
                }

                @Test
                void shouldReturnConflict_WhenSessionExists() throws Exception {
                        // Arrange
                        String requestBody = objectMapper
                                        .writeValueAsString(
                                                        SessionRequestDto.builder().name("New Chest Session").build());

                        // Act & Assert (try to create jan 1 2025, which already exists)
                        mockMvc.perform(post("/sessions/2025-01-01").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                        .contentType("application/json").content(requestBody))
                                        .andExpect(status().isConflict());
                }

                @Test
                void shouldReturnBadRequest_WhenIncomingRequestBodyDoesntMatchDto() throws Exception {
                        // Arrange Act & Assert
                        String requestBody1 = "{\"name\": 10.32}"; // number instead of string

                        mockMvc.perform(post("/sessions/2025-01-01").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                        .contentType("application/json").content(requestBody1))
                                        .andExpect(status().isBadRequest());

                        // Arrange Act & Assert
                        String requestBody2 = "{\"name\": 10.32, \"date\": \"2025-10-10\"}"; // unknown property

                        mockMvc.perform(post("/sessions/2025-01-01").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                        .contentType("application/json").content(requestBody2))
                                        .andExpect(status().isBadRequest());

                        // Arrange Act & Assert
                        String requestBody3 = "{}"; // missing property name

                        mockMvc.perform(post("/sessions/2025-01-01").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                        .contentType("application/json").content(requestBody3))
                                        .andExpect(status().isBadRequest());

                        // Arrange Act & Assert
                        String requestBody4 = "{\"name\": \"\"}"; // empty property name

                        mockMvc.perform(post("/sessions/2025-01-01").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                        .contentType("application/json").content(requestBody4))
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        class UpdateSession {
                @Test
                void shouldReturnUpdatedSession_WhenSessionExists() throws Exception {
                        // Arrange
                        String requestBody = objectMapper
                                        .writeValueAsString(
                                                        SessionRequestDto.builder().name("Chest Day Modified").build());

                        // Act & Assert (modify jan 1 2025, which already exists)
                        mockMvc
                                        .perform(patch("/sessions/2025-01-01")
                                                        .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                                        .contentType("application/json").content(requestBody))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.name").value("Chest Day Modified"))
                                        .andExpect(jsonPath("$.date").value("2025-01-01"))
                                        .andExpect(jsonPath("$.id").value(seededSessions.get(0).getId().toString()));
                }

                @Test
                void shouldGiveNotFound_WhenSessionDoesNotExist() throws Exception {
                        // Arrange
                        String requestBody = objectMapper
                                        .writeValueAsString(
                                                        SessionRequestDto.builder().name("Chest Day Modified").build());

                        // Act & Assert (modify jan 3 2025, which does not exist!)
                        mockMvc
                                        .perform(patch("/sessions/2025-01-03")
                                                        .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                                        .contentType("application/json").content(requestBody))
                                        .andExpect(status().isNotFound());
                }

                @Nested
                class DeleteSession {
                        @Test
                        void shouldReturnNocontent_WhenSessionExists() throws Exception {
                                // Act & Assert (delete jan 1 2025, which already exists)
                                mockMvc
                                                .perform(delete("/sessions/2025-01-01")
                                                                .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))))
                                                .andExpect(status().isNoContent());
                        }

                        @Test
                        void shouldGiveNotFound_WhenSessionDoesNotExist() throws Exception {
                                // Act & Assert (delete jan 3 2025, which does not exists)
                                mockMvc
                                                .perform(delete("/sessions/2025-01-03")
                                                                .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))))
                                                .andExpect(status().isNotFound());
                        }
                }

                @Nested
                class Workflows {
                        @Test
                        void shouldReceiveUpdatedSession_WhenCreatingThenUpdating() throws Exception {
                                // Arrange
                                String requestBody = objectMapper
                                                .writeValueAsString(SessionRequestDto.builder().name("New Session")
                                                                .build());

                                // Act (create jan 3 2025, which does not yet exist)
                                mockMvc
                                                .perform(post("/sessions/2025-01-03")
                                                                .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                                                .contentType("application/json").content(requestBody))
                                                .andExpect(status().isCreated());

                                // Act (modify it as well)
                                requestBody = objectMapper
                                                .writeValueAsString(SessionRequestDto.builder().name("Updated Name")
                                                                .build());
                                mockMvc
                                                .perform(patch("/sessions/2025-01-03")
                                                                .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
                                                                .contentType("application/json").content(requestBody))
                                                .andExpect(status().isOk());

                                // Assert (make sure modified session exists in DB)
                                Optional<Session> session = sessionRepo.findByDateAndUid(LocalDate.of(2025, 1, 3),
                                                TEST_UID);
                                assertThat(session).isNotEmpty();
                                assertThat(session.get().getName()).isEqualTo("Updated Name");
                        }
                }
        }
}
