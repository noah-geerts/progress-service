package com.noahgeerts.progress.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.PerformedExercise.CreatePerformedExerciseDto;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedExercise.UpdatePerformedExerciseDto;
import com.noahgeerts.progress.domain.Session.Session;
import com.noahgeerts.progress.exceptions.ConflictException;
import com.noahgeerts.progress.exceptions.ResourceNotFoundException;
import com.noahgeerts.progress.exceptions.UnprocessableEntityException;
import com.noahgeerts.progress.repository.ExerciseRepository;
import com.noahgeerts.progress.repository.PerformedExerciseRepository;
import com.noahgeerts.progress.repository.SessionRepository;

@ExtendWith(MockitoExtension.class)
public class PerformedExerciseServiceTests {

    @Mock
    private PerformedExerciseRepository peRepo;
    @Mock
    private ExerciseRepository exerciseRepo;
    @Mock
    private SessionRepository sessionRepo;

    private PerformedExerciseService underTest;

    @BeforeEach
    void setup() {
        this.underTest = new PerformedExerciseService(peRepo, sessionRepo, exerciseRepo, new ModelMapper());
    }

    private static final String TEST_UID = "test-uid";

    private static final Long TEST_PEID = 1L;
    private static final int TEST_PE_POSITION = 10;

    private static final Long TEST_SSID = 2L;
    private static final String TEST_SESSION_NAME = "Best Session";
    private static final LocalDate TEST_SESSION_DATE = LocalDate.of(2004, 10, 04);

    private static final Long TEST_EID = 3L;
    private static final String TEST_EXERCISE_NAME = "Test Exercise";

    private Session createTestSession() {
        return Session.builder().ssid(TEST_SSID).name(TEST_SESSION_NAME).date(TEST_SESSION_DATE).uid(TEST_UID).performedExercises(new ArrayList<>()).build();
    }

    private Exercise createTestExercise() {
        return Exercise.builder().eid(TEST_EID).name(TEST_EXERCISE_NAME).uid(TEST_UID).build();
    }

    private PerformedExercise createTestPerformedExercise() {
        return PerformedExercise.builder().position(TEST_PE_POSITION).uid(TEST_UID)
                .session(createTestSession()).exercise(createTestExercise()).build();
    }

    @Nested
    class CreatePerformedExercise {

        @Test
        void shouldThrowConflict_whenPEAlreadyExists() {
            // Arrange (peRepo should return an existing PE for the given ssid, position,
            // and user)
            when(peRepo.findBySession_SsidAndPositionAndUid(TEST_SSID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.of(createTestPerformedExercise()));

            // Act & Assert
            CreatePerformedExerciseDto dto = CreatePerformedExerciseDto.builder().eid(TEST_EID).ssid(TEST_SSID)
                    .position(TEST_PE_POSITION).build();
            assertThatThrownBy(() -> underTest.createPerformedExercise(TEST_UID, dto)).isInstanceOf(ConflictException.class);
        }

        @Test
        void shouldThrowUnprocessableEntity_WhenSsidIsInvalid() {
            // Arrange (peRepo -> empty, exerciseRepo -> valid, sessionRepo -> invalid)
            when(peRepo.findBySession_SsidAndPositionAndUid(TEST_SSID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.empty());
            when(exerciseRepo.findByEidAndUid(TEST_EID, TEST_UID)).thenReturn(Optional.of(createTestExercise()));
            when(sessionRepo.findBySsidAndUid(TEST_SSID, TEST_UID)).thenReturn(Optional.empty());

            // Act & Assert
            CreatePerformedExerciseDto dto = CreatePerformedExerciseDto.builder().eid(TEST_EID).ssid(TEST_SSID)
                    .position(TEST_PE_POSITION).build();
            assertThatThrownBy(() -> underTest.createPerformedExercise(TEST_UID, dto))
                    .isInstanceOf(UnprocessableEntityException.class);
        }

        @Test
        void shouldThrowUnprocessableEntity_WhenEidIsInvalid() {
            // Arrange (peRepo -> empty, exerciseRepo -> invalid, sessionRepo -> valid)
            when(peRepo.findBySession_SsidAndPositionAndUid(TEST_SSID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.empty());
            when(exerciseRepo.findByEidAndUid(TEST_EID, TEST_UID)).thenReturn(Optional.empty());
            when(sessionRepo.findBySsidAndUid(TEST_SSID, TEST_UID)).thenReturn(Optional.of(createTestSession()));

            // Act & Assert
            CreatePerformedExerciseDto dto = CreatePerformedExerciseDto.builder().eid(TEST_EID).ssid(TEST_SSID)
                    .position(TEST_PE_POSITION).build();
            assertThatThrownBy(() -> underTest.createPerformedExercise(TEST_UID, dto))
                    .isInstanceOf(UnprocessableEntityException.class);
        }

        @Test
        void shouldReturnCreatedExercise_WhenEverythingValid() {
            // Arrange (peRepo -> empty, exerciseRepo -> valid, sessionRepo -> valid)
            when(peRepo.findBySession_SsidAndPositionAndUid(TEST_SSID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.empty());
            when(exerciseRepo.findByEidAndUid(TEST_EID, TEST_UID)).thenReturn(Optional.of(createTestExercise()));
            when(sessionRepo.findBySsidAndUid(TEST_SSID, TEST_UID)).thenReturn(Optional.of(createTestSession()));

            PerformedExercise testPE = createTestPerformedExercise();
            when(peRepo.save(testPE)).thenReturn(testPE);

            // Act
            CreatePerformedExerciseDto dto = CreatePerformedExerciseDto.builder().eid(TEST_EID).ssid(TEST_SSID)
                    .position(TEST_PE_POSITION).build();
            underTest.createPerformedExercise(TEST_UID, dto);
            verify(peRepo).save(testPE);
        }
    }

    @Nested
    class updatePerformedExercise {

        @Test
        public void shouldThrowNotFound_WhenPEDoesNotExist() {
            // Arrange (peRepo doesnt find the existing PE)
            when(peRepo.findByPeidAndUid(TEST_PEID, TEST_UID)).thenReturn(Optional.empty());

            // Act & Assert
            UpdatePerformedExerciseDto dto = UpdatePerformedExerciseDto.builder().eid(TEST_EID).build();
            assertThatThrownBy(() -> underTest.updatePerformedExercise(TEST_UID, TEST_PEID, dto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        public void shouldThrowUnprocessableEntity_WhenEidIsInvalid() {
            // Arrange (peRepo finds the PE, exerciseRepo finds nothing)
            when(peRepo.findByPeidAndUid(TEST_PEID, TEST_UID)).thenReturn(Optional.of(createTestPerformedExercise()));
            when(exerciseRepo.findByEidAndUid(TEST_EID, TEST_UID)).thenReturn(Optional.empty());

            // Act & Assert
            UpdatePerformedExerciseDto dto = UpdatePerformedExerciseDto.builder().eid(TEST_EID).build();
            assertThatThrownBy(() -> underTest.updatePerformedExercise(TEST_UID, TEST_PEID, dto))
                    .isInstanceOf(UnprocessableEntityException.class);
        }

        @Test
        public void shouldReturnUpdatedExercise_WhenEverythingValid() {
            // Arrange (peRepo finds the PE, exerciseRepo finds an exercise, save returns an
            // updated PerformedExercise)
            when(peRepo.findByPeidAndUid(TEST_PEID, TEST_UID)).thenReturn(Optional.of(createTestPerformedExercise()));
            when(exerciseRepo.findByEidAndUid(TEST_EID, TEST_UID)).thenReturn(Optional.of(createTestExercise()));
            when(peRepo.save(createTestPerformedExercise())).thenReturn(createTestPerformedExercise());

            // Act & Assert
            UpdatePerformedExerciseDto dto = UpdatePerformedExerciseDto.builder().eid(TEST_EID).build();
            underTest.updatePerformedExercise(TEST_UID, TEST_PEID, dto);
        }
    }

    @Nested
    class DeletePerformedExercise {

        @Test
        public void shouldThrowNotFound_WhenPEDoesNotExist() {
            // Arrange (peRepo doesnt find the existing PE)
            when(peRepo.findByPeidAndUid(TEST_PEID, TEST_UID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> underTest.deletePerformedExercise(TEST_UID, TEST_PEID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        public void shouldRunSuccessfully_WhenPEExists() {
            // Arrange (peRepo finds the PE)
            when(peRepo.findByPeidAndUid(TEST_PEID, TEST_UID)).thenReturn(Optional.of(createTestPerformedExercise()));

            // Act & Assert
            underTest.deletePerformedExercise(TEST_UID, TEST_PEID);
            verify(peRepo).delete(createTestPerformedExercise());
        }
    }
}
