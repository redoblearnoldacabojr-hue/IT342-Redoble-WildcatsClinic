package edu.cit.redoble.features.appointment.service;

import edu.cit.redoble.features.appointment.entity.AppointmentEntity;
import edu.cit.redoble.features.appointment.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public List<AppointmentEntity> getForUser(Long userId) {
        return appointmentRepository.findByPatientId(userId);
    }

    public AppointmentEntity create(AppointmentEntity appt) {
        // check conflict
        LocalDate date = appt.getDate();
        LocalTime time = appt.getTime();
        if (appointmentRepository.findByPatientIdAndDateAndTime(appt.getPatientId(), date, time).isPresent()) {
            throw new IllegalArgumentException("Appointment conflict");
        }

        return appointmentRepository.save(appt);
    }
}
