package edu.cit.redoble.features.record.controller;

import edu.cit.redoble.features.record.entity.MedicalRecordEntity;
import edu.cit.redoble.features.record.repository.MedicalRecordRepository;
import edu.cit.redoble.features.auth.entity.UserEntity;
import edu.cit.redoble.features.auth.repository.UserRepository;
import edu.cit.redoble.features.shared.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        List<MedicalRecordEntity> list = medicalRecordRepository.findByPatientId(user.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (MedicalRecordEntity r : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("summary", r.getSummary());
            m.put("date", r.getDate());
            out.add(m);
        }

        return ResponseEntity.ok(out);
    }

    private UserEntity resolveCurrentUser(Authentication authentication, HttpServletRequest request) {
        String email = null;

        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank() && !"anonymousUser".equals(authentication.getName())) {
            email = authentication.getName().trim();
        }

        if (email == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                try {
                    email = jwtService.extractUsername(token);
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
}
