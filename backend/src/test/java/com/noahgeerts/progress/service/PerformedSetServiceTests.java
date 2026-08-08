package com.noahgeerts.progress.service;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedSet.CreatePerformedSetDto;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSet;
import com.noahgeerts.progress.domain.PerformedSet.PerformedSetResponseDto;
import com.noahgeerts.progress.domain.PerformedSet.UpdatePerformedSetDto;
import com.noahgeerts.progress.exceptions.ConflictException;
import com.noahgeerts.progress.exceptions.ResourceNotFoundException;
import com.noahgeerts.progress.exceptions.UnprocessableEntityException;
import com.noahgeerts.progress.repository.PerformedExerciseRepository;
import com.noahgeerts.progress.repository.PerformedSetRepository;

@ExtendWith(MockitoExtension.class)
public class PerformedSetServiceTests {

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
        when(setRepo.findByPerformedExercise_PeidAndPositionAndUid(0L, 0, "uid"))
                .thenReturn(Optional.of(PerformedSet.builder().build()));

        // Act & Assert
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().peid(0L).position(0).build();
        assertThatThrownBy(() -> underTest.createPerformedSet("uid", dto)).isInstanceOf(ConflictException.class);
    }

    @Test
    public void createPerformedSet_InvalidPeid_ThrowsUnprocessable() {
        // Arrange (find set method returns empty, but peRepo find by id also finds
        // nothing)
        when(setRepo.findByPerformedExercise_PeidAndPositionAndUid(0L, 0, "uid"))
                .thenReturn(Optional.empty());
        when(peRepo.findByPeidAndUid(0L, "uid")).thenReturn(Optional.empty());

        // Act & Assert
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().peid(0L).position(0).build();
        assertThatThrownBy(() -> underTest.createPerformedSet("uid", dto)).isInstanceOf(UnprocessableEntityException.class);
    }

    @Test
    public void createPerformedSet_ValidPeidDoesntExist_ReturnsNewEntity() {
        // Arrange (find set method returns empty and peRepo find by id returns a valid
        // PerformedExercise)
        PerformedExercise exercise = PerformedExercise.builder().build();
        when(setRepo.findByPerformedExercise_PeidAndPositionAndUid(0L, 0, "uid"))
                .thenReturn(Optional.empty());
        when(peRepo.findByPeidAndUid(0L, "uid")).thenReturn(Optional.of(exercise));
        PerformedSet newExercise = PerformedSet.builder().weight(20.2).reps(10).position(0).uid("uid").performedExercise(exercise)
                .build();
        when(setRepo.save(newExercise)).thenReturn(newExercise);

        // Act
        CreatePerformedSetDto dto = CreatePerformedSetDto.builder().peid(0L).weight(20.2).reps(10).position(0).build();
        PerformedSetResponseDto result = underTest.createPerformedSet("uid", dto);

        // Assert6
        PerformedSetResponseDto expected = PerformedSetResponseDto.builder().weight(20.2).reps(10).position(0).build();
        assertThat(expected).isEqualTo(result);
    }

    @Test
    public void updatePerformedSet_NoSuchSetExists_ThrowsNotFound() {
        // Arrange (set repo doesn't find the set)
        when(setRepo.findByStidAndUid(0L, "uid")).thenReturn(Optional.empty());

        // Act & Assert
        UpdatePerformedSetDto dto = UpdatePerformedSetDto.builder().build();
        assertThatThrownBy(() -> underTest.updatePerformedSet("uid", 0L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void updatePerformedSet_SetExists_UpdatesSuccessfully() {
        // Arrange (set repo does find the set)
        PerformedSet oldSet = PerformedSet.builder().weight(10.1).position(5).reps(5).stid(0L).build();
        PerformedSet newSet = PerformedSet.builder().weight(20.2).position(5).reps(10).stid(0L).build();
        when(setRepo.findByStidAndUid(0L, "uid")).thenReturn(Optional.of(oldSet));
        when(setRepo.save(newSet)).thenReturn(newSet);

        // Act
        UpdatePerformedSetDto dto = UpdatePerformedSetDto.builder().weight(newSet.getWeight()).reps(newSet.getReps())
                .build();
        PerformedSetResponseDto result = underTest.updatePerformedSet("uid", 0L, dto);

        // Assert
        PerformedSetResponseDto expected = PerformedSetResponseDto.builder().weight(20.2).position(5).reps(10).stid(0L).build();
        assertThat(expected).isEqualTo(result);
    }

    @Test
    public void deletePerformedSet_SetDoesntExist_ThrowsNotFound() {
        // Arrange (set repo doesn't find the set)
        when(setRepo.findByStidAndUid(0L, "uid")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> underTest.deletePerformedSet("uid", 0L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void deletePerformedSet_SetExists_DeletesSuccessfully() {
        // Arrange (set repo does find the set)
        PerformedExercise pe = PerformedExercise.builder().position(0).sets(new ArrayList<>()).build();
        PerformedSet oldSet = PerformedSet.builder().weight(10.1).reps(5).stid(0L).performedExercise(pe).build();
        when(setRepo.findByStidAndUid(0L, "uid")).thenReturn(Optional.of(oldSet));

        // Act & Assert
        underTest.deletePerformedSet("uid", 0L);
        verify(setRepo).delete(oldSet);
    }

}
