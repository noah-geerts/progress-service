package com.noahgeerts.progress.service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedSet.CreatePerformedSetDto;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSetResponseDto;
import com.noahgeerts.progress.domain.PerformedSet.UpdatePerformedSetDto;
import com.noahgeerts.progress.exceptions.BadRequestException;
import com.noahgeerts.progress.exceptions.ConflictException;
import com.noahgeerts.progress.exceptions.ResourceNotFoundException;
import com.noahgeerts.progress.exceptions.UnprocessableEntityException;
import com.noahgeerts.progress.repository.PerformedExerciseRepository;
import com.noahgeerts.progress.repository.PerformedSetRepository;

@ExtendWith(MockitoExtension.class)
public class PerformedSetServiceTests {

    private static final UUID TEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private PerformedSetRepository setRepo;
    @Mock
    private PerformedExerciseRepository peRepo;

    private PerformedSetService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new PerformedSetService(setRepo, peRepo, new ModelMapper());
    }

    @Test
    public void createPerformedSet_AlreadyExists_ThrowsConflict() {
        // Arrange (find set method should return an existing PerformedSet)
        when(setRepo.findByPerformedExercise_IdAndPositionAndUid(TEST_ID, 0, "uid"))
                .thenReturn(Optional.of(PerformedSet.builder().build()));

        // Act & Assert
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().performedExerciseId(TEST_ID).position(0).build();
        assertThatThrownBy(() -> underTest.createPerformedSet("uid", dto)).isInstanceOf(ConflictException.class);
    }

    @Test
    public void createPerformedSet_InvalidPerformedExerciseId_ThrowsUnprocessable() {
        // Arrange (find set method returns empty, but peRepo find by id also finds
        // nothing)
        when(setRepo.findByPerformedExercise_IdAndPositionAndUid(TEST_ID, 0, "uid"))
                .thenReturn(Optional.empty());
        when(peRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.empty());

        // Act & Assert
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().performedExerciseId(TEST_ID).position(0).build();
        assertThatThrownBy(() -> underTest.createPerformedSet("uid", dto)).isInstanceOf(UnprocessableEntityException.class);
    }

    @Test
    public void createPerformedSet_ValidPerformedExerciseId_ReturnsNewEntity() {
        // Arrange (find set method returns empty and peRepo find by id returns a valid
        // PerformedExercise)
        PerformedExercise exercise = PerformedExercise.builder().id(TEST_ID).build();
        when(setRepo.findByPerformedExercise_IdAndPositionAndUid(TEST_ID, 0, "uid"))
                .thenReturn(Optional.empty());
        when(peRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(exercise));
        PerformedSet newSet = PerformedSet.builder().weight(20.2).reps(10).position(0).uid("uid").performedExercise(exercise)
                .build();
        when(setRepo.save(argThat(performedSet -> 
            performedSet.getPerformedExercise().getId().equals(newSet.getPerformedExercise().getId())
            && performedSet.getWeight() == newSet.getWeight()
            && performedSet.getReps() == newSet.getReps()
            && performedSet.getPosition() == newSet.getPosition()
            && performedSet.getUid() == newSet.getUid()
        ))).thenReturn(newSet);

        // Act
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().performedExerciseId(TEST_ID).weight(20.2).reps(10).position(0).build();
        PerformedSetResponseDto result = underTest.createPerformedSet("uid", dto);

        // Assert
        PerformedSetResponseDto expected = PerformedSetResponseDto.builder().weight(20.2).reps(10).position(0).build();
        assertThat(expected).isEqualTo(result);
    }

        @Test
        public void createOrUpdatePerformedSet_SetDoesNotExist_CreatesWithProvidedId() {
        UUID performedExerciseId = UUID.randomUUID();
        PerformedExercise exercise = PerformedExercise.builder().id(performedExerciseId).build();
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().performedExerciseId(performedExerciseId)
            .position(2).reps(3).weight(215.0).build();
        PerformedSet expectedSet = PerformedSet.builder().id(TEST_ID).performedExercise(exercise)
            .position(2).reps(3).weight(215.0).uid("uid").build();
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.empty());
        when(setRepo.findByPerformedExercise_IdAndPositionAndUid(performedExerciseId, 2, "uid"))
            .thenReturn(Optional.empty());
        when(peRepo.findByIdAndUid(performedExerciseId, "uid")).thenReturn(Optional.of(exercise));
        when(setRepo.save(expectedSet)).thenReturn(expectedSet);

        ResponseEntity<PerformedSetResponseDto> result = underTest.createOrUpdatePerformedSet("uid", TEST_ID, dto);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isEqualTo(PerformedSetResponseDto.builder().id(TEST_ID)
            .position(2).reps(3).weight(215.0).build());
        verify(setRepo).save(expectedSet);
        }

        @Test
        public void createOrUpdatePerformedSet_SetDoesNotExistAndInvalidPerformedExerciseId_ThrowsUnprocessable() {
        UUID performedExerciseId = UUID.randomUUID();
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().performedExerciseId(performedExerciseId)
            .position(2).reps(3).weight(215.0).build();
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.empty());
        when(setRepo.findByPerformedExercise_IdAndPositionAndUid(performedExerciseId, 2, "uid"))
            .thenReturn(Optional.empty());
        when(peRepo.findByIdAndUid(performedExerciseId, "uid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.createOrUpdatePerformedSet("uid", TEST_ID, dto))
            .isInstanceOf(UnprocessableEntityException.class);
        verify(setRepo, never()).save(any(PerformedSet.class));
        }

        @Test
        public void createOrUpdatePerformedSet_SetDoesNotExistAndPositionAlreadyUsed_ThrowsConflict() {
        UUID performedExerciseId = UUID.randomUUID();
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().performedExerciseId(performedExerciseId)
            .position(1).reps(3).weight(215.0).build();
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.empty());
        when(setRepo.findByPerformedExercise_IdAndPositionAndUid(performedExerciseId, 1, "uid"))
            .thenReturn(Optional.of(PerformedSet.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> underTest.createOrUpdatePerformedSet("uid", TEST_ID, dto))
            .isInstanceOf(ConflictException.class);
        verify(setRepo, never()).save(any(PerformedSet.class));
        }

        @Test
        public void createOrUpdatePerformedSet_SetExists_UpdatesSuccessfully() {
        UUID performedExerciseId = UUID.randomUUID();
        PerformedExercise exercise = PerformedExercise.builder().id(performedExerciseId).build();
        PerformedSet oldSet = PerformedSet.builder().id(TEST_ID).performedExercise(exercise)
            .position(0).reps(5).weight(225.0).uid("uid").build();
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().performedExerciseId(performedExerciseId)
            .position(0).reps(12).weight(197.7).build();
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(oldSet));
        when(setRepo.save(oldSet)).thenReturn(oldSet);

        ResponseEntity<PerformedSetResponseDto> result = underTest.createOrUpdatePerformedSet("uid", TEST_ID, dto);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(PerformedSetResponseDto.builder().id(TEST_ID)
            .position(0).reps(12).weight(197.7).build());
        assertThat(oldSet.getPerformedExercise()).isSameAs(exercise);
        assertThat(oldSet.getUid()).isEqualTo("uid");
        assertThat(oldSet.getPosition()).isZero();
        assertThat(oldSet.getReps()).isEqualTo(12);
        assertThat(oldSet.getWeight()).isEqualTo(197.7);
        verify(setRepo).save(oldSet);
        }

        @Test
        public void createOrUpdatePerformedSet_SetExistsAndPerformedExerciseChanged_ThrowsBadRequest() {
        PerformedExercise exercise = PerformedExercise.builder().id(UUID.randomUUID()).build();
        PerformedSet oldSet = PerformedSet.builder().id(TEST_ID).performedExercise(exercise)
            .position(0).reps(5).weight(225.0).uid("uid").build();
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().performedExerciseId(UUID.randomUUID())
            .position(0).reps(12).weight(197.7).build();
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(oldSet));

        assertThatThrownBy(() -> underTest.createOrUpdatePerformedSet("uid", TEST_ID, dto))
            .isInstanceOf(BadRequestException.class);
        verify(setRepo, never()).save(any(PerformedSet.class));
        assertThat(oldSet).isEqualTo(PerformedSet.builder().id(TEST_ID).performedExercise(exercise)
            .position(0).reps(5).weight(225.0).uid("uid").build());
        }

        @Test
        public void createOrUpdatePerformedSet_SetExistsAndPositionChanged_ThrowsBadRequest() {
        UUID performedExerciseId = UUID.randomUUID();
        PerformedExercise exercise = PerformedExercise.builder().id(performedExerciseId).build();
        PerformedSet oldSet = PerformedSet.builder().id(TEST_ID).performedExercise(exercise)
            .position(0).reps(5).weight(225.0).uid("uid").build();
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().performedExerciseId(performedExerciseId)
            .position(99).reps(12).weight(197.7).build();
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(oldSet));

        assertThatThrownBy(() -> underTest.createOrUpdatePerformedSet("uid", TEST_ID, dto))
            .isInstanceOf(BadRequestException.class);
        verify(setRepo, never()).save(any(PerformedSet.class));
        assertThat(oldSet).isEqualTo(PerformedSet.builder().id(TEST_ID).performedExercise(exercise)
            .position(0).reps(5).weight(225.0).uid("uid").build());
        }

        @Test
        public void updatePerformedSet_NoSuchSetExists_ThrowsNotFound() {
        // Arrange (set repo doesn't find the set)
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.empty());

        // Act & Assert
        UpdatePerformedSetDto dto = UpdatePerformedSetDto.builder().build();
        assertThatThrownBy(() -> underTest.updatePerformedSet("uid", TEST_ID, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void updatePerformedSet_SetExists_UpdatesSuccessfully() {
        // Arrange (set repo does find the set)
        PerformedSet oldSet = PerformedSet.builder().weight(10.1).position(5).reps(5).id(TEST_ID).build();
        PerformedSet newSet = PerformedSet.builder().weight(20.2).position(5).reps(10).id(TEST_ID).build();
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(oldSet));
        when(setRepo.save(newSet)).thenReturn(newSet);

        // Act
        UpdatePerformedSetDto dto = UpdatePerformedSetDto.builder().weight(newSet.getWeight()).reps(newSet.getReps())
                .build();
        PerformedSetResponseDto result = underTest.updatePerformedSet("uid", TEST_ID, dto);

        // Assert
        PerformedSetResponseDto expected = PerformedSetResponseDto.builder().weight(20.2).position(5).reps(10).id(TEST_ID).build();
        assertThat(expected).isEqualTo(result);
    }

    @Test
    public void deletePerformedSet_SetDoesntExist_ThrowsNotFound() {
        // Arrange (set repo doesn't find the set)
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> underTest.deletePerformedSet("uid", TEST_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void deletePerformedSet_SetExists_DeletesSuccessfully() {
        // Arrange (set repo does find the set)
        PerformedExercise pe = PerformedExercise.builder().position(0).sets(new ArrayList<>()).build();
        PerformedSet oldSet = PerformedSet.builder().weight(10.1).reps(5).id(TEST_ID).performedExercise(pe).build();
        when(setRepo.findByIdAndUid(TEST_ID, "uid")).thenReturn(Optional.of(oldSet));

        // Act & Assert
        underTest.deletePerformedSet("uid", TEST_ID);
        verify(setRepo).delete(oldSet);
    }

}
