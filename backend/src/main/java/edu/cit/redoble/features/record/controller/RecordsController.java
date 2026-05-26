package edu.cit.redoble.features.record.controller;

import edu.cit.redoble.features.record.entity.MedicalRecordEntity;
import edu.cit.redoble.features.record.repository.MedicalRecordRepository;
import edu.cit.redoble.features.auth.entity.UserEntity;
import edu.cit.redoble.features.auth.entity.UserRole;
import edu.cit.redoble.features.auth.repository.UserRepository;
import edu.cit.redoble.features.shared.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/records")
public class RecordsController {

    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public RecordsController(MedicalRecordRepository medicalRecordRepository, UserRepository userRepository, JwtService jwtService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication authentication, HttpServletRequest request) {
        UserEntity user = resolveCurrentUser(authentication, request);
        List<MedicalRecordEntity> list = UserRole.isPrivileged(user.getRole())
                ? medicalRecordRepository.findAll()
                : medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(user.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (MedicalRecordEntity r : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("summary", r.getSummary());
            m.put("date", r.getDate());
            m.put("doctorName", r.getDoctorName());
            m.put("remarks", r.getRemarks());
            m.put("results", r.getResults());
            m.put("completedAt", r.getCompletedAt());
            m.put("appointmentId", r.getAppointmentId());
            out.add(m);
        }

        return ResponseEntity.ok(out);
    }

    @PatchMapping("/{recordId}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long recordId,
                                                      @Valid @RequestBody UpdateRecordRequest request,
                                                      Authentication authentication,
                                                      HttpServletRequest httpRequest) {
        UserEntity user = resolveCurrentUser(authentication, httpRequest);
        if (!UserRole.isPrivileged(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff or admin access required");
        }

        MedicalRecordEntity record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));

        record.setTitle(request.getTitle().trim());
        record.setSummary(request.getSummary().trim());
        record.setDate(request.getDate().trim());
        record.setRemarks(request.getRemarks());
        record.setResults(request.getResults());
        MedicalRecordEntity saved = medicalRecordRepository.save(record);

        return ResponseEntity.ok(mapRecord(saved));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> delete(@PathVariable Long recordId,
                                       Authentication authentication,
                                       HttpServletRequest httpRequest) {
        UserEntity user = resolveCurrentUser(authentication, httpRequest);
        if (!UserRole.isAdmin(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        if (!medicalRecordRepository.existsById(recordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
        }

        medicalRecordRepository.deleteById(recordId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> mapRecord(MedicalRecordEntity r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("title", r.getTitle());
        m.put("summary", r.getSummary());
        m.put("date", r.getDate());
        m.put("doctorName", r.getDoctorName());
        m.put("remarks", r.getRemarks());
        m.put("results", r.getResults());
        m.put("completedAt", r.getCompletedAt());
        m.put("appointmentId", r.getAppointmentId());
        m.put("patientId", r.getPatientId());
        return m;
    }

    private UserEntity resolveCurrentUser(Authentication authentication, HttpServletRequest request) {
        String email = null;

        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                email = email.trim().toLowerCase();
            }
        }

        if (email == null && authentication != null && authentication.getName() != null && !authentication.getName().isBlank() && !"anonymousUser".equals(authentication.getName())) {
            email = authentication.getName().trim().toLowerCase();
        }

        if (email == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                try {
                    email = jwtService.extractUsername(token).toLowerCase();
                } catch (Exception ignored) {
                    email = null;
                }
            }
        }

        if (email == null || email.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public static class UpdateRecordRequest {
        @NotBlank
        private String title;

        @NotBlank
        private String summary;

        @NotBlank
        private String date;

        private String remarks;
        private String results;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public String getResults() {
            return results;
        }

        public void setResults(String results) {
            this.results = results;
        }
    }
}
