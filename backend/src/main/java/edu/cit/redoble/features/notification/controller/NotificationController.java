package edu.cit.redoble.features.notification.controller;

import edu.cit.redoble.features.auth.entity.UserEntity;
import edu.cit.redoble.features.auth.repository.UserRepository;
import edu.cit.redoble.features.notification.entity.NotificationEntity;
import edu.cit.redoble.features.notification.service.NotificationService;
import edu.cit.redoble.features.shared.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public NotificationController(NotificationService notificationService,
                                  UserRepository userRepository,
                                  JwtService jwtService) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(Authentication authentication, HttpServletRequest request) {
        UserEntity user = resolveCurrentUser(authentication, request);
        List<NotificationEntity> notifications = notificationService.getForUser(user.getId());
        List<Map<String, Object>> out = new ArrayList<>();
        for (NotificationEntity notification : notifications) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", notification.getId());
            item.put("userId", notification.getUserId());
            item.put("message", notification.getMessage());
            item.put("isRead", notification.isRead());
            item.put("createdAt", notification.getCreatedAt());
            out.add(item);
        }

        return ResponseEntity.ok(out);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(Authentication authentication, HttpServletRequest request) {
        UserEntity user = resolveCurrentUser(authentication, request);
        int updated = notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(Map.of("updated", updated));
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