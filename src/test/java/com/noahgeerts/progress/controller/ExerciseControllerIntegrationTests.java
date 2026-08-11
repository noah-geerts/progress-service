package com.noahgeerts.progress.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.Exercise.ExerciseRequestDto;
import com.noahgeerts.progress.domain.Exercise.ExerciseResponseDto;
import com.noahgeerts.progress.repository.ExerciseRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class ExerciseControllerIntegrationTests {
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private ModelMapper mapper;
  @Autowired
  private ExerciseRepository exerciseRepository;

  private List<Exercise> seededExercises;

  private static final String TEST_UID = "test_user";
  private static final String TEST_EXERCISE = "New Exercise";

  @BeforeEach
  void setup() {
    List<Exercise> seedExercises = List.of(Exercise.builder().name("Bench Press").uid(TEST_UID).build(),
        Exercise.builder().name("Dumbell Press").uid(TEST_UID).build(),
        Exercise.builder().name("Squat").uid(TEST_UID).build());
    Iterable<Exercise> saved = exerciseRepository.saveAll(seedExercises);
    this.seededExercises = StreamSupport.stream(saved.spliterator(), false)
        .collect(Collectors.toList());
  }

  @AfterEach
  void teardown () {
    exerciseRepository.deleteAll();
  }

  @Nested
  class GetAllExercises {
    @Test
    void shouldReturnUnauthorized_whenNoUserLoggedIn() throws Exception {
      mockMvc.perform(get("/exercises")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnAllExercises_whenExercisesExistInDB() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))).andExpect(status().isOk())
          .andExpect(jsonPath("$[?(@.name == 'Bench Press')]").exists())
          .andExpect(jsonPath("$[?(@.name == 'Dumbell Press')]").exists())
          .andExpect(jsonPath("$[?(@.name == 'Squat')]").exists());
    }

    @Test
    void shouldReturnNoExercises_whenDifferentUserAuthenticated() throws Exception {
      // Act & Assert
      mockMvc.perform(get("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", "another_user"))))
          .andExpect(status().isOk())
          .andExpect(content().string("[]"));
    }
  }

  @Nested
  class CreateExercise {
    @Test
    void shouldCreateExerciseInDBAndReturnIt_whenRequestValid() throws Exception {
      // Arrange
      ExerciseRequestDto dto = ExerciseRequestDto.builder().name(TEST_EXERCISE).build();
      String requestBody = objectMapper.writeValueAsString(dto);

      // Act
      mockMvc
          .perform(post("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))).contentType("application/json")
              .content(requestBody))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value(TEST_EXERCISE));

      // Assert
      Optional<Exercise> fromDB = exerciseRepository.findByNameAndUid(TEST_EXERCISE, TEST_UID);
      assertThat(fromDB).isNotEmpty();
      assertThat(fromDB.get().getName()).isEqualTo(TEST_EXERCISE);
    }

    @Test
    void shouldReturnConflict_whenExerciseNameAlreadyExists() throws Exception {
      // Arrange
      ExerciseRequestDto dto = ExerciseRequestDto.builder().name("Bench Press").build(); // Bench Press is already
                                                                                         // seeded
      String requestBody = objectMapper.writeValueAsString(dto);

      // Act & Assert
      mockMvc
          .perform(post("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))).contentType("application/json")
              .content(requestBody))
          .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnBadRequest_WhenIncomingRequestBodyDoesntMatchDto() throws Exception {
      String requestBody1 = "{\"name\": 10.32}"; // number instead of string

      mockMvc.perform(post("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
          .contentType("application/json").content(requestBody1)).andExpect(status().isBadRequest());

      // Arrange Act & Assert
      String requestBody2 = "{\"name\": 10.32, \"date\": \"2025-10-10\"}"; // unknown property

      mockMvc.perform(post("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
          .contentType("application/json").content(requestBody2)).andExpect(status().isBadRequest());

      // Arrange Act & Assert
      String requestBody3 = "{}"; // missing property name

      mockMvc.perform(post("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
          .contentType("application/json").content(requestBody3)).andExpect(status().isBadRequest());

      // Arrange Act & Assert
      String requestBody4 = "{\"name\": \"\"}"; // empty property name

      mockMvc.perform(post("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID)))
          .contentType("application/json").content(requestBody4)).andExpect(status().isBadRequest());
    }
  }

  @Nested
  class UpdateExercise {
    @Test
    void shouldUpdateSuccessfullyInDBAndReturnIt_whenRequestValid() throws Exception {
      // Arrange
      ExerciseRequestDto dto = ExerciseRequestDto.builder().name(TEST_EXERCISE).build();
      String requestBody = objectMapper.writeValueAsString(dto);

      // Act
      mockMvc
          .perform(patch("/exercises/" + seededExercises.get(0).getId())
              .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))).contentType("application/json")
              .content(requestBody))
          .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(TEST_EXERCISE));

      // Assert
      Optional<Exercise> fromDB = exerciseRepository.findByIdAndUid(seededExercises.get(0).getId(), TEST_UID);
      assertThat(fromDB).isNotEmpty();
      assertThat(fromDB.get().getName()).isEqualTo(TEST_EXERCISE);
    }

    @Test
    void shouldReturnNotFound_whenEidDoesntExist() throws Exception {
      // Arrange
      ExerciseRequestDto dto = ExerciseRequestDto.builder().name(TEST_EXERCISE).build();
      String requestBody = objectMapper.writeValueAsString(dto);

      // Act & Assert
      mockMvc.perform(patch("/exercises/" + UUID.randomUUID())
          .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))).contentType("application/json")
          .content(requestBody))
          .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFound_ifEidBelongsToAnotherUser() throws Exception {
      // Arrange
      ExerciseRequestDto dto = ExerciseRequestDto.builder().name(TEST_EXERCISE).build();
      String requestBody = objectMapper.writeValueAsString(dto);

      // Act & Assert
      mockMvc.perform(patch("/exercises/" + seededExercises.get(0).getId())
          .with(jwt().jwt(jwt -> jwt.claim("sub", "another user"))).contentType("application/json")
          .content(requestBody))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  class DeleteExercise {
    @Test
    void shouldReturnNotFound_whenEidDoesntExist() throws Exception {
      // Arrange
      ExerciseRequestDto dto = ExerciseRequestDto.builder().name(TEST_EXERCISE).build();
      String requestBody = objectMapper.writeValueAsString(dto);

      // Act & Assert
      mockMvc.perform(delete("/exercises/" + UUID.randomUUID())
          .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))).contentType("application/json")
          .content(requestBody))
          .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNoContentAndDeleteInDB_whenRequestValid() throws Exception {
      // Act
      Exercise toDelete = seededExercises.get(0);
      mockMvc.perform(delete("/exercises/" + toDelete.getId())
          .with(jwt().jwt(jwt -> jwt.claim("sub", TEST_UID))))
          .andExpect(status().isNoContent());

      // Assert
      Optional<Exercise> deleted = exerciseRepository.findById(toDelete.getId());
      assertThat(deleted).isEmpty();
    }
  }

  @Nested
  class Workflows {
    @Test
    void shouldGetNewExercisesForThisUser_whenAddingThenDeletingThenGetting() throws Exception {
      // Arrange
      List<ExerciseRequestDto> toAdd = List.of(ExerciseRequestDto.builder().name("Exercise 1").build(),
          ExerciseRequestDto.builder().name("Exercise 2").build());

      // Act (add both in toAdd, then delete the first one)
      String thisTestUser = "this_user";
      ResultActions firstCreate = mockMvc
          .perform(
              post("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", thisTestUser))).contentType("application/json")
                  .content(objectMapper.writeValueAsString(toAdd.get(0))))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value(toAdd.get(0).getName()));
      mockMvc
          .perform(
              post("/exercises").with(jwt().jwt(jwt -> jwt.claim("sub", thisTestUser))).contentType("application/json")
                  .content(objectMapper.writeValueAsString(toAdd.get(1))))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value(toAdd.get(1).getName()));

      // Deletion
      String firstCreateResponseBody = firstCreate.andReturn().getResponse().getContentAsString();
      ExerciseResponseDto firstCreateResponse = objectMapper.readValue(firstCreateResponseBody,
          ExerciseResponseDto.class);
      mockMvc.perform(delete("/exercises/" + firstCreateResponse.getId())
          .with(jwt().jwt(jwt -> jwt.claim("sub", thisTestUser))))
          .andExpect(status().isNoContent());

      // Assert
      mockMvc.perform(get("/exercises")
          .with(jwt().jwt(jwt -> jwt.claim("sub", thisTestUser))))
          .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].name").value("Exercise 2"));

    }
  }
}
