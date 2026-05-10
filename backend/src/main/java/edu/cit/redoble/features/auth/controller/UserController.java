package edu.cit.redoble.features.auth.controller;

import edu.cit.redoble.features.auth.dto.UpdateStaffStatusRequest;
import edu.cit.redoble.features.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PatchMapping("/{userId}/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStaffStatus(@PathVariable Long userId,
                                                   @Valid @RequestBody UpdateStaffStatusRequest request,
                                                   Authentication authentication) {
        authService.updateStaffStatus(userId, request.getIsStaff(), authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
