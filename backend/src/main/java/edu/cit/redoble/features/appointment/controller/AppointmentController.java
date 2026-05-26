package edu.cit.redoble.features.appointment.controller;

import edu.cit.redoble.features.appointment.dto.CreateAppointmentRequest;
import edu.cit.redoble.features.appointment.dto.UpdateAppointmentStatusRequest;
import edu.cit.redoble.features.appointment.entity.AppointmentEntity;
import edu.cit.redoble.features.appointment.entity.AppointmentStatus;
import edu.cit.redoble.features.appointment.service.AppointmentService;
import edu.cit.redoble.features.auth.entity.UserEntity;
import edu.cit.redoble.features.auth.entity.UserRole;
import edu.cit.redoble.features.auth.repository.UserRepository;
import edu.cit.redoble.features.doctor.entity.DoctorEntity;
import edu.cit.redoble.features.doctor.repository.DoctorRepository;
import edu.cit.redoble.features.shared.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final JwtService jwtService;

    public AppointmentController(AppointmentService appointmentService, UserRepository userRepository, DoctorRepository doctorRepository, JwtService jwtService) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication authentication, HttpServletRequest request) {
        UserEntity user = resolveCurrentUser(authentication, request);
        List<AppointmentEntity> list = UserRole.isPrivileged(user.getRole())
                ? appointmentService.getAll()
                : appointmentService.getForUser(user.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (AppointmentEntity a : list) {
            out.add(mapAppointment(a));
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
        appt.setDate(date);
        appt.setTime(time);
        appt.setReason(req.getReason() == null ? "Appointment" : req.getReason());

        try {
            AppointmentEntity saved = appointmentService.create(appt);
            return ResponseEntity.status(HttpStatus.CREATED).body(mapAppointment(saved));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Conflict: appointment exists"));
        }
    }

    @PatchMapping("/{appointmentId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long appointmentId,
                                                            @Valid @RequestBody UpdateAppointmentStatusRequest request,
                                                            Authentication authentication,
                                                            HttpServletRequest httpRequest) {
        UserEntity user = resolveCurrentUser(authentication, httpRequest);
        if (!UserRole.isPrivileged(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Staff or admin access required"));
        }

        AppointmentEntity updated = appointmentService.updateStatus(
                appointmentId,
                request.getStatus(),
                request.getDoctorId(),
                request.getRemarks(),
                request.getResults()
        );
        return ResponseEntity.ok(mapAppointment(updated));
    }

    private Map<String, Object> mapAppointment(AppointmentEntity appointment) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", appointment.getId());
        item.put("patientId", appointment.getPatientId());
        item.put("patientName", userRepository.findById(appointment.getPatientId())
                .map(patient -> {
                    String fullName = (patient.getFirstName() + " " + patient.getLastName()).trim();
                    return fullName.isBlank() ? patient.getEmail() : fullName;
                })
                .orElse("Unknown patient"));
        item.put("patientEmail", userRepository.findById(appointment.getPatientId())
                .map(UserEntity::getEmail)
                .orElse(null));
        item.put("date", appointment.getDate().toString());
        item.put("time", appointment.getTime().toString());
        item.put("reason", appointment.getReason());
        item.put("doctorId", appointment.getDoctorId());
        item.put("doctorName", appointment.getDoctorId() == null ? null : doctorRepository.findById(appointment.getDoctorId())
                .map(DoctorEntity::getName)
                .orElse(null));
        item.put("status", appointment.getStatus() == null ? AppointmentStatus.PROCESSING.name() : appointment.getStatus().name());
        item.put("completedAt", appointment.getCompletedAt());
        item.put("completionRemarks", appointment.getCompletionRemarks());
        item.put("completionResults", appointment.getCompletionResults());
        return item;
    }

    private UserEntity resolveCurrentUser(Authentication authentication, HttpServletRequest request) {
        String email = null;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                email = jwtService.extractUsername(token).trim().toLowerCase();
            } catch (Exception ignored) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
            }
        }

        if (email == null && authentication != null && authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
            if (email != null && !email.isBlank()) {
                email = email.trim().toLowerCase();
            }
        }

        if (email == null && authentication != null && authentication.getName() != null && !authentication.getName().isBlank() && !"anonymousUser".equals(authentication.getName())) {
            email = authentication.getName().trim();
        }

        if (email == null || email.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
