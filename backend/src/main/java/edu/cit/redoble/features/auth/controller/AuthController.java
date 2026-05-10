package edu.cit.redoble.features.auth.controller;

import edu.cit.redoble.features.auth.dto.AuthResponse;
import edu.cit.redoble.features.auth.dto.LoginRequest;
import edu.cit.redoble.features.auth.dto.RegisterRequest;
import edu.cit.redoble.features.auth.entity.UserEntity;
import edu.cit.redoble.features.auth.repository.UserRepository;
import edu.cit.redoble.features.auth.service.AuthService;
import edu.cit.redoble.features.shared.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthController(AuthService authService, UserRepository userRepository, JwtService jwtService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication, HttpServletRequest request) {
        String email = resolveEmail(authentication, request);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Optional<UserEntity> userLookup = userRepository.findByEmailIgnoreCase(email.trim());
        if (userLookup.isPresent()) {
            UserEntity user = userLookup.get();
            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "provider", user.getProvider().name(),
                    "isStaff", user.isStaff()
            ));
        }

        String token = resolveBearerToken(request);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        Map<String, Object> claims = jwtService.extractClaim(token, claimsMap -> new HashMap<>(claimsMap));
        Object uid = claims.get("uid");
        Object firstName = claims.getOrDefault("firstName", "");
        Object lastName = claims.getOrDefault("lastName", "");
        Object provider = claims.getOrDefault("provider", "LOCAL");
        Object isStaff = claims.getOrDefault("isStaff", false);

        return ResponseEntity.ok(Map.of(
                "id", uid == null ? null : Long.valueOf(String.valueOf(uid)),
                "email", email,
                "firstName", String.valueOf(firstName),
                "lastName", String.valueOf(lastName),
                "provider", String.valueOf(provider),
                "isStaff", Boolean.parseBoolean(String.valueOf(isStaff))
        ));
    }

    private String resolveEmail(Authentication authentication, HttpServletRequest request) {
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank() && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName().trim().toLowerCase();
        }

        String token = resolveBearerToken(request);
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            return jwtService.extractUsername(token).toLowerCase();
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
