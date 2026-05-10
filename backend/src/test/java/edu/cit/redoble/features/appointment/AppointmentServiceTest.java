package edu.cit.redoble.features.appointment;

import edu.cit.redoble.features.appointment.entity.AppointmentEntity;
import edu.cit.redoble.features.appointment.repository.AppointmentRepository;
import edu.cit.redoble.features.appointment.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void createSavesNewAppointmentWhenNoConflictExists() {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setPatientId(1L);
        appointment.setDate(LocalDate.of(2026, 3, 3));
        appointment.setTime(LocalTime.of(10, 0));
        appointment.setReason("General Checkup");

        when(appointmentRepository.findByPatientIdAndDateAndTime(1L, appointment.getDate(), appointment.getTime()))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        AppointmentEntity saved = appointmentService.create(appointment);

        assertEquals(appointment, saved);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void createThrowsWhenConflictExists() {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setPatientId(1L);
        appointment.setDate(LocalDate.of(2026, 3, 3));
        appointment.setTime(LocalTime.of(10, 0));

        when(appointmentRepository.findByPatientIdAndDateAndTime(1L, appointment.getDate(), appointment.getTime()))
                .thenReturn(Optional.of(appointment));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.create(appointment));
    }
}
