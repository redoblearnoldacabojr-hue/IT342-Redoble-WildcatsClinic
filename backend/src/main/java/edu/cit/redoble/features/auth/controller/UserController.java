package edu.cit.redoble.features.auth.controller;

import edu.cit.redoble.features.auth.dto.UpdateRoleRequest;
import edu.cit.redoble.features.auth.entity.UserEntity;
import edu.cit.redoble.features.auth.repository.UserRepository;
import edu.cit.redoble.features.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public UserController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(this::toMap).toList());
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateRole(@PathVariable Long userId,
                                           @Valid @RequestBody UpdateRoleRequest request) {
        authService.updateRole(userId, request.getRole());
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toMap(UserEntity user) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", user.getId());
        item.put("email", user.getEmail());
        item.put("firstName", user.getFirstName());
        item.put("lastName", user.getLastName());
        item.put("role", user.getRole());
        item.put("provider", user.getProvider().name());
        return item;
    }
}
