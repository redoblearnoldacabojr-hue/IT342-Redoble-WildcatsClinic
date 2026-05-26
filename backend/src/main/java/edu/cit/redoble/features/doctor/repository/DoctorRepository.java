package edu.cit.redoble.features.doctor.repository;

import edu.cit.redoble.features.doctor.entity.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<DoctorEntity, Long> {
    List<DoctorEntity> findAllByOrderByNameAsc();
    List<DoctorEntity> findByDoctorInTrueOrderByNameAsc();
}