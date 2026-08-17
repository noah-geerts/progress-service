package com.noahgeerts.progress.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedSet.CreatePerformedSetDto;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;
import com.noahgeerts.progress.domain.PerformedSet.UpdatePerformedSetDto;
import com.noahgeerts.progress.domain.Session.Session;
import com.noahgeerts.progress.repository.ExerciseRepository;
import com.noahgeerts.progress.repository.PerformedExerciseRepository;
import com.noahgeerts.progress.repository.PerformedSetRepository;
import com.noahgeerts.progress.repository.SessionRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class PerformedSetControllerIntegrationTests {

        @Autowired
        private PerformedExerciseRepository peRepo;
        @Autowired
        private ExerciseRepository exerciseRepo;
        @Autowired
        private PerformedSetRepository setRepo;
        @Autowired
        private SessionRepository sessionRepo;

        @Autowired
        private ObjectMapper objectMapper;
        @Autowired
        private MockMvc mockMvc;

        private static final String TEST_UID = "test_user";

        private List<PerformedExercise> seededPEs;
        private List<PerformedSet> seededSets;
        private List<Exercise> seededExercises;
        private List<Session> seededSessions;

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
                                                .uid(TEST_UID).position(1).build(),
                                // Dumbell Press on Chest Day
                                PerformedExercise.builder().exercise(seededExercises.get(1))
                                                .session(seededSessions.get(0))
                                                .uid(TEST_UID).position(2).build(),
                                // Squat on Leg Day
                                PerformedExercise.builder().exercise(seededExercises.get(2))
                                                .session(seededSessions.get(1))
                                                .uid(TEST_UID).position(1).build());
                peRepo.saveAll(seededPEs);

                // Seed sets
                seededSets = List
                                .of(
                                                // Bench 225x5
                                                PerformedSet.builder().reps(5).weight(225.0).position(0)
                                                                .performedExercise(seededPEs.get(0)).uid(TEST_UID)
                                                                .build(),
                                                // Bench 220x5
                                                PerformedSet.builder().reps(5).weight(220.0).position(1)
                                                                .performedExercise(seededPEs.get(0)).uid(TEST_UID)
                                                                .build(),
                                                // Dumbell Press 60sx12
                                                PerformedSet.builder().reps(12).weight(60.0).position(0)
                                                                .performedExercise(seededPEs.get(1)).uid(TEST_UID)
                                                                .build(),
                                                // Squat 315x3
                                                PerformedSet.builder().reps(3).weight(315.0).position(0)
                                                                .performedExercise(seededPEs.get(2)).uid(TEST_UID)
                                                                .build(),
                                                // Squat 315x2
                                                PerformedSet.builder().reps(2).weight(315.0).position(1)
                                                                .performedExercise(seededPEs.get(2)).uid(TEST_UID)
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
        class Authentication {

                @Test
                void shouldReturnUnauthorizedOnAllEndpoints_WhenNoAuthToken() throws Exception {
                        mockMvc.perform(post("/sets")).andExpect(status().isUnauthorized());
                        mockMvc.perform(patch("/sets/1234")).andExpect(status().isUnauthorized());
                        mockMvc.perform(delete("/sets/1234")).andExpect(status().isUnauthorized());
                }
        }

        private JwtRequestPostProcessor createTestJWT() {
                return jwt().jwt(jwt -> jwt.claim("sub", TEST_UID));
        }

        @Nested
        class CreatePerformedSet {

                @Test
                void shouldReturnCreatedAndCreateInDB_whenRequestValid() throws Exception {
                        // Arrange
                        UUID performedExerciseId = seededPEs.get(0).getId(); // bench press
                        int position = 2; // not used yet (bench has 2 sets in seed data, 0 and 1)
                        int reps = 3;
                        double weight = 215.0;

                        String requestBody = objectMapper.writeValueAsString(
                                        CreatePerformedSetDto.builder().performedExerciseId(performedExerciseId).position(position).reps(reps)
                                                        .weight(weight).build());

                        // Act
                        MvcResult result = mockMvc
                                        .perform(post("/sets").with(createTestJWT()).contentType("application/json")
                                                        .content(requestBody))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.position").value(position))
                                        .andExpect(jsonPath("$.reps").value(reps))
                                        .andExpect(jsonPath("$.weight").value(weight))
                                        .andReturn();

                        // Assert (make sure it was created)
                        String content = result.getResponse().getContentAsString();
                        String idValue = JsonPath.read(content, "$.id");
                        UUID id = UUID.fromString(idValue);
                        Optional<PerformedSet> created = setRepo.findById(id);
                        assertThat(created).isNotEmpty();
                        assertThat(created.get().getPosition()).isEqualTo(position);
                        assertThat(created.get().getReps()).isEqualTo(reps);
                        assertThat(created.get().getWeight()).isEqualTo(weight);
                }

                @Test
                void shouldReturnConflict_whenSetAlreadyExistsWithPositionAndPerformedExerciseId() throws Exception {
                        // Arrange
                        UUID performedExerciseId = seededPEs.get(0).getId(); // bench press
                        int position = 1; // already used (bench has 2 sets in seed data, 0 and 1)
                        int reps = 3;
                        double weight = 215.0;

                        String requestBody = objectMapper.writeValueAsString(
                                        CreatePerformedSetDto.builder().performedExerciseId(performedExerciseId).position(position).reps(reps)
                                                        .weight(weight).build());

                        // Act
                        mockMvc.perform(post("/sets").with(createTestJWT()).contentType("application/json")
                                        .content(requestBody))
                                        .andExpect(status().isConflict());

                        // Assert (make sure nothing was created)
                        Iterable<PerformedSet> inDb = setRepo.findAll();
                        assertThat(inDb).hasSize(5);
                }

                @Test
                void shouldReturnUnprocessable_whenThePerformedExerciseIdProvidedIsInvalid() throws Exception {
                        // Arrange
                        UUID performedExerciseId = UUID.randomUUID(); // Not a real performed exercise
                        int position = 2;
                        int reps = 3;
                        double weight = 215.0;

                        String requestBody = objectMapper.writeValueAsString(
                                        CreatePerformedSetDto.builder().performedExerciseId(performedExerciseId).position(position).reps(reps)
                                                        .weight(weight).build());

                        // Act
                        mockMvc.perform(post("/sets").with(createTestJWT()).contentType("application/json")
                                        .content(requestBody))
                                        .andExpect(status().isUnprocessableEntity());

                        // Assert (make sure nothing was created)
                        Iterable<PerformedSet> inDb = setRepo.findAll();
                        assertThat(inDb).hasSize(seededSets.size());
                }

                @ParameterizedTest
                @ValueSource(strings = {
                                "{\"performedExerciseId\": \"00000000-0000-0000-0000-000000000001\", \"position\": 1, \"reps\": 1, \"weight\": \"shouldn't be a string\"}", // weight
                                                                                                                      // wrong
                                // datatype
                                "{\"performedExerciseId\": \"00000000-0000-0000-0000-000000000001\", \"position\": 1, \"reps\": \"string\", \"weight\": 225.0}", // reps wrong
                                                                                                           // datatype
                                "{\"performedExerciseId\": \"00000000-0000-0000-0000-000000000001\", \"position\": \"hi\", \"reps\": 1, \"weight\": 225.0}", // position wrong
                                                                                                       // datatype
                                "{\"performedExerciseId\": \"hi\", \"position\": 1, \"reps\": 1, \"weight\": 225.0}", // performed exercise id wrong
                                                                                                       // datatype
                                "{\"performedExerciseId\": \"00000000-0000-0000-0000-000000000001\", \"position\": 1, \"reps\": 1}", // missing weight
                                "{\"performedExerciseId\": \"00000000-0000-0000-0000-000000000001\", \"position\": 1, \"weight\": 225.0}", // missing reps
                                "{\"performedExerciseId\": \"00000000-0000-0000-0000-000000000001\", \"reps\": 1, \"weight\": 225.0}", // missing position
                                "{\"position\": 1, \"reps\": 1, \"weight\": 225.0}", // missing performed exercise id
                                "{\"performedExerciseId\": \"00000000-0000-0000-0000-000000000001\", \"position\": 1, \"reps\": 1, \"weight\": 225.0, \"extra\": 0}" // extra
                                                                                                               // field
                })
                void shouldReturnBadRequest_whenRequestBodyDoesNotMatchDto(String requestBody) throws Exception {
                        mockMvc.perform(post("/sets").with(createTestJWT()).contentType("application/json")
                                        .content(requestBody))
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        class UpdatePerformedSet {

                @Test
                void shouldReturnOkAndUpdateInDB_whenRequestValid() throws Exception {
                        // Arrange
                        UUID id = seededSets.get(0).getId();
                        int position = seededSets.get(0).getPosition();
                        int reps = 20;
                        double weight = 100.0;

                        String requestBody = objectMapper.writeValueAsString(
                                        UpdatePerformedSetDto.builder().reps(reps).weight(weight).build());

                        // Act
                        mockMvc.perform(patch("/sets/" + id).with(createTestJWT()).contentType("application/json")
                                        .content(requestBody))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.position").value(position))
                                        .andExpect(jsonPath("$.reps").value(reps))
                                        .andExpect(jsonPath("$.weight").value(weight))
                                        .andExpect(jsonPath("$.id").value(id.toString()));

                        // Assert (make sure it was updated)
                        Optional<PerformedSet> updatedInDb = setRepo.findById(id);
                        assertThat(updatedInDb).isNotEmpty();
                        assertThat(updatedInDb.get().getReps()).isEqualTo(reps);
                        assertThat(updatedInDb.get().getWeight()).isEqualTo(weight);
                }

                @Test
                void shouldReturnNotFound_whenSetDoesNotExist() throws Exception {
                        // Arrange
                        UUID id = UUID.randomUUID();
                        int reps = 20;
                        double weight = 100.0;

                        String requestBody = objectMapper.writeValueAsString(
                                        UpdatePerformedSetDto.builder().reps(reps).weight(weight).build());

                        // Act
                        mockMvc.perform(patch("/sets/" + id).with(createTestJWT()).contentType("application/json")
                                        .content(requestBody))
                                        .andExpect(status().isNotFound());
                }

                @ParameterizedTest
                @ValueSource(strings = {
                                "{\"reps\": 10, \"weight\": \"string\"}", // Weight is wrong datatype
                                "{\"reps\": \"dad\", \"weight\": 10}", // Reps is wrong datatype
                                "{\"reps\": 10", // missing weight
                                "{\"weight\": 10}", // missing reps
                                "{\"reps\": 10, \"weight\": 10, \"extra\": 0}" // Extra field
                })
                void shouldReturnBadRequest_whenRequestBodyDoesNotMatchDto(String requestBody) throws Exception {
                        mockMvc.perform(patch("/sets/" + seededSets.get(0).getId()).with(createTestJWT())
                                                        .contentType("application/json").content(requestBody))
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        class DeletePerformedSet {

                @Test
                void shouldReturnNoContentAndDeleteInDB_whenRequestedSetExists() throws Exception {
                        // Arrange
                        UUID id = seededSets.get(0).getId();

                        // Act
                        mockMvc.perform(
                                        delete("/sets/" + id).with(createTestJWT()))
                                        .andExpect(status().isNoContent());

                        // Assert (make sure it was deleted)
                        Optional<PerformedSet> deletedInDB = setRepo.findById(id);
                        assertThat(deletedInDB).isEmpty();
                }

                @Test
                void shouldReturnNotFound_whenIdIsNotValid() throws Exception {
                        // Arrange
                        UUID id = UUID.randomUUID();

                        // Act
                        mockMvc.perform(
                                        delete("/sets/" + id).with(createTestJWT()))
                                        .andExpect(status().isNotFound());

                        // Assert (make sure nothing was deleted)
                        Iterable<PerformedSet> inDB = setRepo.findAll();
                        assertThat(inDB).hasSize(seededSets.size());
                }
        }

        @Nested
        class Workflows {

                @Test
                void shouldUpdateDBCorrectly_whenCreatingUpdatingDeleting() throws Exception {
                        // Arrange
                        UUID performedExerciseId = seededPEs.get(0).getId(); // bench press
                        int position = 2; // not used yet (bench has 2 sets in seed data, 0 and 1)
                        int reps = 3;
                        double weight = 215.0;

                        String requestBody = objectMapper.writeValueAsString(
                                        CreatePerformedSetDto.builder().performedExerciseId(performedExerciseId).position(position).reps(reps)
                                                        .weight(weight).build());

                        // Act (create a third set)
                        MvcResult result = mockMvc
                                        .perform(post("/sets").with(createTestJWT()).contentType("application/json")
                                                        .content(requestBody))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.position").value(position))
                                        .andExpect(jsonPath("$.reps").value(reps))
                                        .andExpect(jsonPath("$.weight").value(weight))
                                        .andReturn();

                        // Assert (make sure it was created)
                        String content = result.getResponse().getContentAsString();
                        String idValue = JsonPath.read(content, "$.id");
                        UUID id = UUID.fromString(idValue);
                        Optional<PerformedSet> created = setRepo.findById(id);
                        assertThat(created).isNotEmpty();
                        assertThat(created.get().getPosition()).isEqualTo(position);
                        assertThat(created.get().getReps()).isEqualTo(reps);
                        assertThat(created.get().getWeight()).isEqualTo(weight);

                        // Arrange
                        reps = 5;
                        weight = 200.0;
                        requestBody = objectMapper.writeValueAsString(
                                        UpdatePerformedSetDto.builder().reps(reps).weight(weight).build());

                        // Act (update the reps and weight)
                        mockMvc.perform(patch("/sets/" + id).with(createTestJWT()).contentType("application/json")
                                        .content(requestBody)).andExpect(status().isOk())
                                        .andExpect(jsonPath("$.position").value(position))
                                        .andExpect(jsonPath("$.reps").value(reps))
                                        .andExpect(jsonPath("$.weight").value(weight));

                        // Assert (make sure it was updated in the DB)
                        Optional<PerformedSet> updated = setRepo.findById(id);
                        assertThat(updated).isNotEmpty();
                        assertThat(updated.get().getPosition()).isEqualTo(position);
                        assertThat(updated.get().getReps()).isEqualTo(reps);
                        assertThat(updated.get().getWeight()).isEqualTo(weight);

                        // Act (delete it)
                        mockMvc.perform(delete("/sets/" + id).with(createTestJWT()))
                                        .andExpect(status().isNoContent());

                        // Assert (make sure it was deleted from the DB)
                        Optional<PerformedSet> deleted = setRepo.findById(id);
                        assertThat(deleted).isEmpty();
                }
        }

}
