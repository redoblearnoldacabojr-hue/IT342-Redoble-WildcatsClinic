package edu.cit.redoble.features.doctor.controller;

import edu.cit.redoble.features.doctor.entity.DoctorEntity;
import edu.cit.redoble.features.doctor.service.DoctorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(doctorService.getAllDoctors().stream().map(this::toMap).toList());
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<Map<String, Object>>> available() {
        return ResponseEntity.ok(doctorService.getAvailableDoctors().stream().map(this::toMap).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody DoctorRequest request) {
        DoctorEntity doctor = new DoctorEntity();
        doctor.setName(request.getName().trim());
        doctor.setSpecialization(request.getSpecialization().trim());
        doctor.setDoctorIn(resolveDoctorIn(request));
        DoctorEntity saved = doctorService.create(doctor);
        return ResponseEntity.ok(toMap(saved));
    }

    @PatchMapping("/{doctorId}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long doctorId, @Valid @RequestBody DoctorRequest request) {
        DoctorEntity saved = doctorService.update(
                doctorId,
                request.getName().trim(),
                request.getSpecialization().trim(),
                resolveDoctorIn(request)
        );
        return ResponseEntity.ok(toMap(saved));
    }

    @DeleteMapping("/{doctorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long doctorId) {
        doctorService.delete(doctorId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(DoctorEntity doctor) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", doctor.getId());
        item.put("name", doctor.getName());
        item.put("specialization", doctor.getSpecialization());
        item.put("available", doctor.isAvailable());
        item.put("doctorIn", doctor.isDoctorIn());
        item.put("status", doctor.isDoctorIn() ? "IN" : "OUT");
        item.put("createdAt", doctor.getCreatedAt());
        item.put("updatedAt", doctor.getUpdatedAt());
        return item;
    }

    private boolean resolveDoctorIn(DoctorRequest request) {
        if (request.getDoctorIn() != null) {
            return request.getDoctorIn();
        }

        return request.getAvailable() == null || request.getAvailable();
    }

    public static class DoctorRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String specialization;
        private Boolean doctorIn;
        @NotNull
        private Boolean available;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSpecialization() {
            return specialization;
        }

        public void setSpecialization(String specialization) {
            this.specialization = specialization;
        }

        public Boolean getDoctorIn() {
            return doctorIn;
        }

        public void setDoctorIn(Boolean doctorIn) {
            this.doctorIn = doctorIn;
        }

        public Boolean getAvailable() {
            return available;
        }

        public void setAvailable(Boolean available) {
            this.available = available;
        }
    }
}