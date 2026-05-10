package edu.cit.redoble.features.appointment.controller;

import edu.cit.redoble.features.appointment.dto.CreateAppointmentRequest;
import edu.cit.redoble.features.appointment.entity.AppointmentEntity;
import edu.cit.redoble.features.appointment.service.AppointmentService;
import edu.cit.redoble.features.auth.entity.UserEntity;
import edu.cit.redoble.features.auth.repository.UserRepository;
import edu.cit.redoble.features.shared.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AppointmentController(AppointmentService appointmentService, UserRepository userRepository, JwtService jwtService) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication authentication, HttpServletRequest request) {
        UserEntity user = resolveCurrentUser(authentication, request);
        List<AppointmentEntity> list = appointmentService.getForUser(user.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (AppointmentEntity a : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("patientId", a.getPatientId());
            m.put("patientName", a.getPatientName());
            m.put("date", a.getDate().toString());
            m.put("time", a.getTime().toString());
            m.put("reason", a.getReason());
            out.add(m);
        }

        return ResponseEntity.ok(out);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateAppointmentRequest req, Authentication authentication, HttpServletRequest request) {
        UserEntity user = resolveCurrentUser(authentication, request);

        LocalDate date = LocalDate.parse(req.getDate());
        LocalTime time = LocalTime.parse(req.getTime());

        // validate not in past
        if (date.isBefore(LocalDate.now()) || (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now()))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Cannot book past date/time"));
        }

        AppointmentEntity appt = new AppointmentEntity();
        appt.setPatientId(user.getId());
        appt.setPatientName(user.getFirstName() + " " + user.getLastName());
        appt.setDate(date);
        appt.setTime(time);
        appt.setReason(req.getReason() == null ? "Appointment" : req.getReason());

        try {
            AppointmentEntity saved = appointmentService.create(appt);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", saved.getId(),
                    "patientId", saved.getPatientId(),
                    "patientName", saved.getPatientName(),
                    "date", saved.getDate().toString(),
                    "time", saved.getTime().toString(),
                    "reason", saved.getReason()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Conflict: appointment exists"));
        }
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
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
