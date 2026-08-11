package com.noahgeerts.progress.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;

import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.Exercise.ExerciseRequestDto;
import com.noahgeerts.progress.domain.Exercise.ExerciseResponseDto;
import com.noahgeerts.progress.exceptions.ConflictException;
import com.noahgeerts.progress.exceptions.ResourceNotFoundException;
import com.noahgeerts.progress.exceptions.UnprocessableEntityException;
import com.noahgeerts.progress.repository.ExerciseRepository;

@ExtendWith(MockitoExtension.class)
public class ExerciseServiceTests {

  private static final UUID TEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock
  private ExerciseRepository exerciseRepo;

  private ExerciseService underTest;

  @BeforeEach
  void setup() {
    // Here we provide a ModelMapper from the library because we do not want to mock
    // the model mapper,
    // which is already well tested and can be treated as a normal library (think
    // ArrayList, which we would not mock)
    underTest = new ExerciseService(exerciseRepo, new ModelMapper());
  }

  @Test
  public void getAllExercises_DBReturnsExercises_returnsCorrectDtos() {
    // Arrange
    List<Exercise> exercises = List.of(Exercise.builder().name("Exercise 1").uid("test uid").build(),
        Exercise.builder().name("Exercise 2").uid("test uid").build());
    when(exerciseRepo.findAllByUidOrderByNameAsc("doesn't matter as long as these two match")).thenReturn(exercises);

    List<ExerciseResponseDto> expected = List.of(ExerciseResponseDto.builder().name("Exercise 1").build(),
        ExerciseResponseDto.builder().name("Exercise 2").build());

    // Act
    List<ExerciseResponseDto> result = underTest.getAllExercises("doesn't matter as long as these two match");

    // Assert
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void createExercise_DBFindExistingExercise_throwsConflict() {
    // Arrange
    Optional<Exercise> alreadyExists = Optional
      .of(Exercise.builder().name("doesn't matter").uid("the uid").id(TEST_ID).build());
    when(exerciseRepo.findByNameAndUid("Exercise already exists", "the uid"))
        .thenReturn(alreadyExists);

    // Act & Assert
    ExerciseRequestDto dto = ExerciseRequestDto.builder().name("Exercise already exists").build();
    assertThatThrownBy(
        () -> underTest.createExercise("the uid", dto))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  public void createExercise_DBFindsNoExistingExercise_returnsNewExerciseDto() {
    // Arrange
    when(exerciseRepo.findByNameAndUid("New Exercise", "the uid"))
        .thenReturn(Optional.empty());
    Exercise newExercise = Exercise.builder().name("New Exercise").uid("the uid").build();
    when(exerciseRepo.save(newExercise)).thenReturn(newExercise);

    // Assert
    ExerciseRequestDto dto = ExerciseRequestDto.builder().name("New Exercise").build();
    ExerciseResponseDto result = underTest.createExercise("the uid", dto);
    ExerciseResponseDto expected = ExerciseResponseDto.builder().name("New Exercise").build();

    // Assert
    assertThat(result.getName()).isEqualTo(expected.getName());
  }

  @Test
  public void updateExercise_DBFindsNoSuchEid_throwsNotFound() {
    // Arrange (find by id and uid should return an empty optional)
    when(exerciseRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.empty());

    // Act & Assert (call the method)
    assertThatThrownBy(
        () -> underTest.updateExercise("uid", ExerciseRequestDto.builder().name("Update name").build(), TEST_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void updateExercise_DBFindsExistingExerciseWithName_throwsConflict() {
    // Arrange (find by id and uid should return an exercise, but find by name and
    // uid
    // should also find one already)
    when(exerciseRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(new Exercise()));
    when(exerciseRepo.findByNameAndUid("Updated name", "uid"))
        .thenReturn(Optional.of(Exercise.builder().name("Updated name").uid("uid").build()));

    // Act & Assert (call the method)
    assertThatThrownBy(
        () -> underTest.updateExercise("uid", ExerciseRequestDto.builder().name("Updated name").build(), TEST_ID))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  public void updateExercise_EidValidAndNameNotTaken_returnsUpdatedExerciseDto() {
    // Arrange (find by id and uid should return an exercise, but find by name and
    // uid shouldn't
    Exercise updated = Exercise.builder().name("Updated name").id(TEST_ID).build();
    when(exerciseRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(updated));
    when(exerciseRepo.findByNameAndUid("Updated name", "uid"))
        .thenReturn(Optional.empty());
    when(exerciseRepo.save(updated)).thenReturn(updated);

    // Act
    ExerciseResponseDto result = underTest.updateExercise("uid",
        ExerciseRequestDto.builder().name("Updated name").build(), TEST_ID);

    // Assert
    ExerciseResponseDto expected = ExerciseResponseDto.builder().id(TEST_ID).name("Updated name").build();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void deleteExercise_DBFindsNoSuchEid_throwsNotFound() {
    // Arrange (find by uid and eid should return empty)
    when(exerciseRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> underTest.deleteExercise("uid", TEST_ID)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  public void deleteExercise_DBThrowsDataIntegrityViolation_throwsUnprocessable() {
    // Arrange (find by uid and eid should return the old exercise, delete should
    // throw a DataIntegrityViolationException)
    Exercise toDelete = Exercise.builder().name("Updated name").id(TEST_ID).build();
    when(exerciseRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(toDelete));
    doThrow(new DataIntegrityViolationException("Exercise is still referenced in at least one Performed Exercise"))
        .when(exerciseRepo).delete(toDelete);

    // Act & Assert
    assertThatThrownBy(() -> underTest.deleteExercise("uid", TEST_ID)).isInstanceOf(UnprocessableEntityException.class);
  }

  @Test
  public void deleteExercise_DBDeletesSuccessfully_throwsNothing() {
    // Arrange (find by uid and eid should return the old exercise)
    Exercise toDelete = Exercise.builder().name("Updated name").id(TEST_ID).build();
    when(exerciseRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(toDelete));

    // Act & Assert (as long as nothing is thrown we're good)
    underTest.deleteExercise("uid", TEST_ID);
    verify(exerciseRepo).delete(toDelete);
  }
}
