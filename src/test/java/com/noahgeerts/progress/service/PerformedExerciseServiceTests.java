package com.noahgeerts.progress.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;

import com.noahgeerts.progress.domain.Exercise.Exercise;
import com.noahgeerts.progress.domain.PerformedExercise.CreatePerformedExerciseDto;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExercise;
import com.noahgeerts.progress.domain.PerformedExercise.PerformedExerciseResponseDto;
import com.noahgeerts.progress.domain.PerformedExercise.UpdatePerformedExerciseDto;
import com.noahgeerts.progress.domain.Session.Session;
import com.noahgeerts.progress.exceptions.BadRequestException;
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

    private static final UUID TEST_PERFORMED_EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final int TEST_PE_POSITION = 10;

    private static final UUID TEST_SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final String TEST_SESSION_NAME = "Best Session";
    private static final LocalDate TEST_SESSION_DATE = LocalDate.of(2004, 10, 04);

    private static final UUID TEST_EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final String TEST_EXERCISE_NAME = "Test Exercise";

    private Session createTestSession() {
        return Session.builder().id(TEST_SESSION_ID).name(TEST_SESSION_NAME).date(TEST_SESSION_DATE).uid(TEST_UID).performedExercises(new ArrayList<>()).build();
    }

    private Exercise createTestExercise() {
        return Exercise.builder().id(TEST_EXERCISE_ID).name(TEST_EXERCISE_NAME).uid(TEST_UID).build();
    }

    private PerformedExercise createTestPerformedExercise() {
        return PerformedExercise.builder().id(TEST_PERFORMED_EXERCISE_ID).position(TEST_PE_POSITION).uid(TEST_UID)
                .session(createTestSession()).exercise(createTestExercise()).build();
    }

    @Nested
    class CreatePerformedExercise {

        @Test
        void shouldThrowConflict_whenPEAlreadyExists() {
            // Arrange (peRepo should return an existing performed exercise for the given session id, position,
            // and user)
            when(peRepo.findBySession_IdAndPositionAndUid(TEST_SESSION_ID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.of(createTestPerformedExercise()));

            // Act & Assert
            CreatePerformedExerciseDto dto = CreatePerformedExerciseDto.builder().exerciseId(TEST_EXERCISE_ID).sessionId(TEST_SESSION_ID)
                    .position(TEST_PE_POSITION).build();
            assertThatThrownBy(() -> underTest.createPerformedExercise(TEST_UID, dto)).isInstanceOf(ConflictException.class);
        }

        @Test
        void shouldThrowUnprocessableEntity_WhenSessionIdIsInvalid() {
            // Arrange (peRepo -> empty, exerciseRepo -> valid, sessionRepo -> invalid)
            when(peRepo.findBySession_IdAndPositionAndUid(TEST_SESSION_ID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.empty());
            when(exerciseRepo.findByIdAndUid(TEST_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(createTestExercise()));
            when(sessionRepo.findByIdAndUid(TEST_SESSION_ID, TEST_UID)).thenReturn(Optional.empty());

            // Act & Assert
            CreatePerformedExerciseDto dto = CreatePerformedExerciseDto.builder().exerciseId(TEST_EXERCISE_ID).sessionId(TEST_SESSION_ID)
                    .position(TEST_PE_POSITION).build();
            assertThatThrownBy(() -> underTest.createPerformedExercise(TEST_UID, dto))
                    .isInstanceOf(UnprocessableEntityException.class);
        }

        @Test
        void shouldThrowUnprocessableEntity_WhenExerciseIdIsInvalid() {
            // Arrange (peRepo -> empty, exerciseRepo -> invalid, sessionRepo -> valid)
            when(peRepo.findBySession_IdAndPositionAndUid(TEST_SESSION_ID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.empty());
            when(exerciseRepo.findByIdAndUid(TEST_EXERCISE_ID, TEST_UID)).thenReturn(Optional.empty());
            when(sessionRepo.findByIdAndUid(TEST_SESSION_ID, TEST_UID)).thenReturn(Optional.of(createTestSession()));

            // Act & Assert
            CreatePerformedExerciseDto dto = CreatePerformedExerciseDto.builder().exerciseId(TEST_EXERCISE_ID).sessionId(TEST_SESSION_ID)
                    .position(TEST_PE_POSITION).build();
            assertThatThrownBy(() -> underTest.createPerformedExercise(TEST_UID, dto))
                    .isInstanceOf(UnprocessableEntityException.class);
        }

        @Test
        void shouldReturnCreatedExercise_WhenEverythingValid() {
            // Arrange (peRepo -> empty, exerciseRepo -> valid, sessionRepo -> valid)
            when(peRepo.findBySession_IdAndPositionAndUid(TEST_SESSION_ID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.empty());
            when(exerciseRepo.findByIdAndUid(TEST_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(createTestExercise()));
            when(sessionRepo.findByIdAndUid(TEST_SESSION_ID, TEST_UID)).thenReturn(Optional.of(createTestSession()));

            when(peRepo.save(any(PerformedExercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            CreatePerformedExerciseDto dto = CreatePerformedExerciseDto.builder().exerciseId(TEST_EXERCISE_ID).sessionId(TEST_SESSION_ID)
                    .position(TEST_PE_POSITION).build();
            PerformedExerciseResponseDto result = underTest.createPerformedExercise(TEST_UID, dto);
            ArgumentCaptor<PerformedExercise> saved = ArgumentCaptor.forClass(PerformedExercise.class);
            verify(peRepo).save(saved.capture());
            assertThat(saved.getValue().getId()).isNotNull();
            assertThat(result.getId()).isEqualTo(saved.getValue().getId());
            assertThat(saved.getValue()).usingRecursiveComparison().ignoringFields("id")
                    .isEqualTo(createTestPerformedExercise());
        }
    }

    @Nested
    class CreateOrUpdatePerformedExercise {
        private CreatePerformedExerciseDto createDto() {
            return CreatePerformedExerciseDto.builder().sessionId(TEST_SESSION_ID).exerciseId(TEST_EXERCISE_ID)
                    .position(TEST_PE_POSITION).build();
        }

        @Test
        void shouldCreateWithProvidedId_whenNotFound() {
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.empty());
            when(sessionRepo.findByIdAndUid(TEST_SESSION_ID, TEST_UID)).thenReturn(Optional.of(createTestSession()));
            when(exerciseRepo.findByIdAndUid(TEST_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(createTestExercise()));
            when(peRepo.save(any(PerformedExercise.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var result = underTest.createOrUpdatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, createDto());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody().getId()).isEqualTo(TEST_PERFORMED_EXERCISE_ID);
            assertThat(result.getBody().getPosition()).isEqualTo(TEST_PE_POSITION);
            assertThat(result.getBody().getExercise().getId()).isEqualTo(TEST_EXERCISE_ID);
            verify(peRepo).save(createTestPerformedExercise());
        }

        @Test
        void shouldThrowUnprocessable_whenSessionNotOwnedOrMissing() {
            when(exerciseRepo.findByIdAndUid(TEST_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(createTestExercise()));
            when(sessionRepo.findByIdAndUid(TEST_SESSION_ID, TEST_UID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.createOrUpdatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, createDto()))
                    .isInstanceOf(UnprocessableEntityException.class);
            verify(peRepo, never()).save(any());
        }

        @Test
        void shouldThrowUnprocessable_whenExerciseNotOwnedOrMissing() {
            when(sessionRepo.findByIdAndUid(TEST_SESSION_ID, TEST_UID)).thenReturn(Optional.of(createTestSession()));
            when(exerciseRepo.findByIdAndUid(TEST_EXERCISE_ID, TEST_UID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> underTest.createOrUpdatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, createDto()))
                    .isInstanceOf(UnprocessableEntityException.class);
            verify(peRepo, never()).save(any());
        }

        @Test
        void shouldThrowConflict_whenCreatingAtOccupiedPosition() {
            when(peRepo.findBySession_IdAndPositionAndUid(TEST_SESSION_ID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.of(createTestPerformedExercise()));

            assertThatThrownBy(() -> underTest.createOrUpdatePerformedExercise(TEST_UID, UUID.randomUUID(), createDto()))
                    .isInstanceOf(ConflictException.class);
            verify(peRepo, never()).save(any());
        }

        @Test
        void shouldThrowConflict_whenIdBelongsToAnotherUser() {
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.empty());
            when(peRepo.existsById(TEST_PERFORMED_EXERCISE_ID)).thenReturn(true);

            assertThatThrownBy(() -> underTest.createOrUpdatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, createDto()))
                    .isInstanceOf(ConflictException.class);
            verify(peRepo, never()).save(any());
        }

        @Test
        void shouldUpdatePosition_whenFound() {
            PerformedExercise existing = createTestPerformedExercise();
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(existing));
            when(peRepo.save(existing)).thenReturn(existing);
            CreatePerformedExerciseDto dto = createDto();
            dto.setPosition(0);

            var result = underTest.createOrUpdatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, dto);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getId()).isEqualTo(TEST_PERFORMED_EXERCISE_ID);
            assertThat(result.getBody().getPosition()).isZero();
            assertThat(existing).usingRecursiveComparison().ignoringFields("position").isEqualTo(createTestPerformedExercise());
            verify(peRepo).save(existing);
        }

        @Test
        void shouldReturnOk_whenRequestRepeated() {
            PerformedExercise existing = createTestPerformedExercise();
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(existing));
            when(peRepo.findBySession_IdAndPositionAndUid(TEST_SESSION_ID, TEST_PE_POSITION, TEST_UID))
                    .thenReturn(Optional.of(existing));
            when(peRepo.save(existing)).thenReturn(existing);

            var result = underTest.createOrUpdatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, createDto());

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody().getPosition()).isEqualTo(TEST_PE_POSITION);
            verify(peRepo).save(existing);
        }

        @Test
        void shouldThrowBadRequest_whenSessionChanged() {
            PerformedExercise existing = createTestPerformedExercise();
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(existing));
            CreatePerformedExerciseDto dto = createDto();
            dto.setSessionId(UUID.randomUUID());
            dto.setPosition(0);

            assertThatThrownBy(() -> underTest.createOrUpdatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, dto))
                    .isInstanceOf(BadRequestException.class);
            verify(peRepo, never()).save(any());
            assertThat(existing).isEqualTo(createTestPerformedExercise());
        }

        @Test
        void shouldThrowBadRequest_whenExerciseChanged() {
            PerformedExercise existing = createTestPerformedExercise();
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(existing));
            CreatePerformedExerciseDto dto = createDto();
            dto.setExerciseId(UUID.randomUUID());
            dto.setPosition(0);

            assertThatThrownBy(() -> underTest.createOrUpdatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, dto))
                    .isInstanceOf(BadRequestException.class);
            verify(peRepo, never()).save(any());
            assertThat(existing).isEqualTo(createTestPerformedExercise());
        }

        @Test
        void shouldThrowConflict_whenUpdatingToOccupiedPosition() {
            PerformedExercise existing = createTestPerformedExercise();
            PerformedExercise other = createTestPerformedExercise();
            other.setId(UUID.randomUUID());
            other.setPosition(0);
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(existing));
            when(peRepo.findBySession_IdAndPositionAndUid(TEST_SESSION_ID, 0, TEST_UID)).thenReturn(Optional.of(other));
            CreatePerformedExerciseDto dto = createDto();
            dto.setPosition(0);

            assertThatThrownBy(() -> underTest.createOrUpdatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, dto))
                    .isInstanceOf(ConflictException.class);
            verify(peRepo, never()).save(any());
            assertThat(existing).isEqualTo(createTestPerformedExercise());
        }
    }

    @Nested
    class updatePerformedExercise {

        @Test
        public void shouldThrowNotFound_WhenPEDoesNotExist() {
            // Arrange (peRepo doesnt find the existing PE)
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.empty());

            // Act & Assert
            UpdatePerformedExerciseDto dto = UpdatePerformedExerciseDto.builder().exerciseId(TEST_EXERCISE_ID).build();
            assertThatThrownBy(() -> underTest.updatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, dto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        public void shouldThrowUnprocessableEntity_WhenExerciseIdIsInvalid() {
            // Arrange (peRepo finds the PE, exerciseRepo finds nothing)
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(createTestPerformedExercise()));
            when(exerciseRepo.findByIdAndUid(TEST_EXERCISE_ID, TEST_UID)).thenReturn(Optional.empty());

            // Act & Assert
            UpdatePerformedExerciseDto dto = UpdatePerformedExerciseDto.builder().exerciseId(TEST_EXERCISE_ID).build();
            assertThatThrownBy(() -> underTest.updatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, dto))
                    .isInstanceOf(UnprocessableEntityException.class);
        }

        @Test
        public void shouldReturnUpdatedExercise_WhenEverythingValid() {
            // Arrange (peRepo finds the PE, exerciseRepo finds an exercise, save returns an
            // updated PerformedExercise)
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(createTestPerformedExercise()));
            when(exerciseRepo.findByIdAndUid(TEST_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(createTestExercise()));
            when(peRepo.save(createTestPerformedExercise())).thenReturn(createTestPerformedExercise());

            // Act & Assert
            UpdatePerformedExerciseDto dto = UpdatePerformedExerciseDto.builder().exerciseId(TEST_EXERCISE_ID).build();
            underTest.updatePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID, dto);
        }
    }

    @Nested
    class DeletePerformedExercise {

        @Test
        public void shouldThrowNotFound_WhenPEDoesNotExist() {
            // Arrange (peRepo doesnt find the existing PE)
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> underTest.deletePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        public void shouldRunSuccessfully_WhenPEExists() {
            // Arrange (peRepo finds the PE)
            when(peRepo.findByIdAndUid(TEST_PERFORMED_EXERCISE_ID, TEST_UID)).thenReturn(Optional.of(createTestPerformedExercise()));

            // Act & Assert
            underTest.deletePerformedExercise(TEST_UID, TEST_PERFORMED_EXERCISE_ID);
            verify(peRepo).delete(createTestPerformedExercise());
        }
    }
}
