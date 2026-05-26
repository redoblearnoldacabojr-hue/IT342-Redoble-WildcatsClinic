package edu.cit.redoble.features.doctor.service;

import edu.cit.redoble.features.doctor.entity.DoctorEntity;
import edu.cit.redoble.features.doctor.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public List<DoctorEntity> getAllDoctors() {
        return doctorRepository.findAllByOrderByNameAsc();
    }

    public List<DoctorEntity> getAvailableDoctors() {
        return doctorRepository.findByDoctorInTrueOrderByNameAsc();
    }

    public DoctorEntity create(DoctorEntity doctor) {
        return doctorRepository.save(doctor);
    }

    public DoctorEntity update(Long doctorId, String name, String specialization, boolean doctorIn) {
        DoctorEntity doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
        doctor.setName(name);
        doctor.setSpecialization(specialization);
        doctor.setDoctorIn(doctorIn);
        return doctorRepository.save(doctor);
    }

    public void delete(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new IllegalArgumentException("Doctor not found");
        }

        doctorRepository.deleteById(doctorId);
    }
}