package com.noahgeerts.progress.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
import com.noahgeerts.progress.domain.PerformedExercise.CreatePerformedExerciseDto;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedExercise.UpdatePerformedExerciseDto;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;
import com.noahgeerts.progress.domain.Session.Session;
import com.noahgeerts.progress.repository.ExerciseRepository;
import com.noahgeerts.progress.repository.PerformedExerciseRepository;
import com.noahgeerts.progress.repository.PerformedSetRepository;
import com.noahgeerts.progress.repository.SessionRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class PerformedExerciseControllerIntegrationTests {

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
                                                PerformedSet.builder().reps(5).weight(225.0)
                                                                .performedExercise(seededPEs.get(0)).uid(TEST_UID)
                                                                .build(),
                                                // Bench 220x5
                                                PerformedSet.builder().reps(5).weight(220.0)
                                                                .performedExercise(seededPEs.get(0)).uid(TEST_UID)
                                                                .build(),
                                                // Dumbell Press 60sx12
                                                PerformedSet.builder().reps(12).weight(60.0)
                                                                .performedExercise(seededPEs.get(1)).uid(TEST_UID)
                                                                .build(),
                                                // Squat 315x3
                                                PerformedSet.builder().reps(3).weight(315.0)
                                                                .performedExercise(seededPEs.get(2)).uid(TEST_UID)
                                                                .build(),
                                                // Squat 315x2
                                                PerformedSet.builder().reps(2).weight(315.0)
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
                        mockMvc.perform(post("/performed-exercises")).andExpect(status().isUnauthorized());
                        mockMvc.perform(patch("/performed-exercises/1234")).andExpect(status().isUnauthorized());
                        mockMvc.perform(delete("/performed-exercises/1234")).andExpect(status().isUnauthorized());
                }
        }

        private JwtRequestPostProcessor createTestJWT() {
                return jwt().jwt(jwt -> jwt.claim("sub", TEST_UID));
        }

        @Nested
        class CreatePerformedExercise {

                @Test
                void shouldReturnCreated_whenEverythingValid() throws Exception {
                        // Arrange
                        Long eid = seededExercises.get(0).getEid();
                        Long ssid = seededSessions.get(0).getSsid();
                        String requestBody = objectMapper.writeValueAsString(CreatePerformedExerciseDto.builder()
                                        .eid(eid).ssid(ssid).position(3).build());

                        // Act & Assert
                        mockMvc.perform(
                                        post("/performed-exercises").with(createTestJWT())
                                                        .contentType("application/json")
                                                        .content(requestBody))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.exercise.eid").value(eid))
                                        .andExpect(jsonPath("$.position").value(3));

                        // Assert the session in the db now has this PE
                        Optional<Session> session = sessionRepo.findById(ssid);
                        assertThat(session).isPresent();
                        assertThat(session.get().getPerformedExercises())
                                        .isNotEmpty()
                                        .anyMatch(pe -> pe.getExercise().getEid().equals(eid) && pe.getPosition() == 3);
                }

                @Test
                void shouldReturnConflict_whenAPEAlreadyExistsInThatSessionWithThatPosition() throws Exception {
                        // Arrange
                        Long eid = seededExercises.get(0).getEid();
                        Long ssid = seededSessions.get(0).getSsid();
                        String requestBody = objectMapper.writeValueAsString(CreatePerformedExerciseDto.builder()
                                        .eid(eid).ssid(ssid).position(1).build()); // Position 1 already taken up in
                        // this session by Bench
                        // Press

                        // Act & Assert
                        mockMvc.perform(
                                        post("/performed-exercises").with(createTestJWT())
                                                        .contentType("application/json")
                                                        .content(requestBody))
                                        .andExpect(status().isConflict());
                }

                @Test
                void shouldReturnUnprocessable_whenEidOrSsidIsInvalid() throws Exception {
                        // Arrange with invalid eid
                        Long eid = Long.MAX_VALUE;
                        Long ssid = seededSessions.get(0).getSsid();
                        String requestBody = objectMapper.writeValueAsString(CreatePerformedExerciseDto.builder()
                                        .eid(eid).ssid(ssid).position(3).build());

                        // Act & Assert
                        mockMvc.perform(
                                        post("/performed-exercises").with(createTestJWT())
                                                        .contentType("application/json")
                                                        .content(requestBody))
                                        .andExpect(status().isUnprocessableEntity());

                        // Arrange with invalid ssid
                        eid = seededExercises.get(0).getEid();
                        ssid = Long.MAX_VALUE;
                        requestBody = objectMapper.writeValueAsString(CreatePerformedExerciseDto.builder()
                                        .eid(eid).ssid(ssid).position(3).build());

                        // Act & Assert
                        mockMvc.perform(
                                        post("/performed-exercises").with(createTestJWT())
                                                        .contentType("application/json")
                                                        .content(requestBody))
                                        .andExpect(status().isUnprocessableEntity());
                }

                @ParameterizedTest
                @ValueSource(strings = {
                                "{}", // missing all fields
                                "{\"eid\": \"100\", \"ssid\": 123, \"position\": 1}", // eid as string instead of Long
                                "{\"eid\": 100, \"ssid\": 123, \"position\": 1, \"extraField\": 0}", // extra field
                                "{\"ssid\": 123, \"position\": 1}", // missing eid
                                "{\"eid\": 123, \"position\": 1}", // missing ssid
                                "{\"ssid\": 123, \"eid\": 1}" // missing position
                })
                void shouldReturnBadRequest_WhenIncomingRequestBodyDoesntMatchDto(String requestBody) throws Exception {
                        mockMvc.perform(
                                        post("/performed-exercises").with(createTestJWT())
                                                        .contentType("application/json")
                                                        .content(requestBody))
                                        .andExpect(status().isBadRequest());
                }

        }

        @Nested
        class UpdatePerformedExercise {

                @Test
                void shouldReturnOk_whenEverythingValid() throws Exception {
                        // Arrange
                        Long eid = seededExercises.get(1).getEid();
                        Long peid = seededPEs.get(0).getPeid();
                        String requestBody = objectMapper
                                        .writeValueAsString(UpdatePerformedExerciseDto.builder().eid(eid).build());

                        // Act
                        mockMvc.perform(patch("/performed-exercises/" + peid).with(createTestJWT())
                                        .contentType("application/json").content(requestBody))
                                        .andExpect(status().isOk()).andExpect(jsonPath("$.peid").value(peid))
                                        .andExpect(jsonPath("$.exercise.eid").value(eid));

                        // Assert (ensure it was updated in the db)
                        Optional<PerformedExercise> updatedPe = peRepo.findById(peid);
                        assertThat(updatedPe).isNotEmpty();
                        assertThat(updatedPe.get().getExercise().getEid()).isEqualTo(eid);
                }

                @Test
                void shouldReturnNotFound_whenPeidIsNotValid() throws Exception {
                        // Arrange
                        Long eid = seededExercises.get(1).getEid();
                        Long peid = Long.MAX_VALUE;
                        String requestBody = objectMapper
                                        .writeValueAsString(UpdatePerformedExerciseDto.builder().eid(eid).build());

                        // Act
                        mockMvc.perform(patch("/performed-exercises/" + peid).with(createTestJWT())
                                        .contentType("application/json").content(requestBody))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void shouldReturnUnprocessable_whenEidIsInvalid() throws Exception {
                        // Arrange
                        Long eid = Long.MAX_VALUE;
                        Long peid = seededPEs.get(0).getPeid();
                        String requestBody = objectMapper
                                        .writeValueAsString(UpdatePerformedExerciseDto.builder().eid(eid).build());

                        // Act
                        mockMvc.perform(patch("/performed-exercises/" + peid).with(createTestJWT())
                                        .contentType("application/json").content(requestBody))
                                        .andExpect(status().isUnprocessableEntity());

                        // Assert (ensure PEID is not updated)
                        Optional<PerformedExercise> peById = peRepo.findById(peid);
                        assertThat(peById.get().getExercise().getEid())
                                        .isEqualTo(seededPEs.get(0).getExercise().getEid());
                }

                @ParameterizedTest
                @ValueSource(strings = { "{}", // missing eid
                                "{\"eid\": 20, \"extraField\": \"hi\"}", // extra field
                                "{\"eid\": \"shouldn't be a string\"}" // eid wrong type
                })
                void shouldReturnBadRequest_whenBodyDoesNotMatchDto(String requestBody) throws Exception {
                        // Missing eid
                        mockMvc.perform(patch("/performed-exercises/" + seededPEs.get(0).getPeid())
                                        .with(createTestJWT())
                                        .contentType("application/json").content(requestBody))
                                        .andExpect(status().isBadRequest());

                }
        }

        @Nested
        class DeletePerformedExercise {
                @Test
                void shouldReturnNoContent_whenValidPeid() throws Exception {
                        // Arrange
                        Long peid = seededPEs.get(0).getPeid();

                        // Act
                        mockMvc.perform(delete("/performed-exercises/" + peid).with(createTestJWT()))
                                        .andExpect(status().isNoContent());

                        // Assert
                        Optional<PerformedExercise> shouldBeDeleted = peRepo.findById(peid);
                        assertThat(shouldBeDeleted).isEmpty();
                }

                @Test
                void shouldReturnNotFound_whenPeidIsNotValid() throws Exception {
                        // Arrange
                        Long peid = Long.MAX_VALUE;

                        // Act
                        mockMvc.perform(delete("/performed-exercises/" + peid).with(createTestJWT()))
                                        .andExpect(status().isNotFound());

                        // Assert (nothing was deleted)
                        Iterable<PerformedExercise> pes = peRepo.findAll();
                        assertThat(pes).hasSize(3);
                }
        }

        @Nested
        class Workflows {
                @Test
                void shouldUpdateDBCorrectly_whenCreatingUpdatingDeleting() throws Exception {
                        // Arrange: create a new PerformedExercise
                        Long eid = seededExercises.get(0).getEid();
                        Long ssid = seededSessions.get(0).getSsid();
                        int position = 3; // not used yet
                        String createRequest = objectMapper.writeValueAsString(
                                        CreatePerformedExerciseDto.builder().eid(eid).ssid(ssid).position(position)
                                                        .build());

                        // Act (create)
                        MvcResult createResult;
                        createResult = mockMvc.perform(
                                        post("/performed-exercises").with(createTestJWT())
                                                        .contentType("application/json").content(createRequest))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.exercise.eid").value(eid))
                                        .andExpect(jsonPath("$.position").value(position))
                                        .andReturn();

                        // Assert (created in DB)
                        String createContent = createResult.getResponse().getContentAsString();
                        Number number = JsonPath.read(createContent, "$.peid");
                        Long peid = number.longValue();
                        Optional<PerformedExercise> created = peRepo.findById(peid);
                        assertThat(created).isNotEmpty();
                        assertThat(created.get().getExercise().getEid()).isEqualTo(eid);
                        assertThat(created.get().getPosition()).isEqualTo(position);

                        // Arrange (update: change exercise)
                        Long newEid = seededExercises.get(1).getEid();
                        String updateRequest = objectMapper.writeValueAsString(
                                        UpdatePerformedExerciseDto.builder().eid(newEid).build());

                        // Act (update)
                        mockMvc.perform(
                                        patch("/performed-exercises/" + peid).with(createTestJWT())
                                                        .contentType("application/json").content(updateRequest))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.peid").value(peid))
                                        .andExpect(jsonPath("$.exercise.eid").value(newEid));

                        // Assert (updated in DB)
                        Optional<PerformedExercise> updated = peRepo.findById(peid);
                        assertThat(updated).isNotEmpty();
                        assertThat(updated.get().getExercise().getEid()).isEqualTo(newEid);

                        // Act (delete)
                        mockMvc.perform(delete("/performed-exercises/" + peid).with(createTestJWT()))
                                        .andExpect(status().isNoContent());

                        // Assert (deleted from DB)
                        Optional<PerformedExercise> deleted = peRepo.findById(peid);
                        assertThat(deleted).isEmpty();
                }
        }

}
