package edu.cit.redoble.features.notification.controller;

import edu.cit.redoble.features.notification.dto.DeviceRegisterRequest;
import edu.cit.redoble.features.notification.entity.DeviceEntity;
import edu.cit.redoble.features.notification.repository.DeviceRepository;
import edu.cit.redoble.features.shared.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final JwtService jwtService;

    public DeviceController(DeviceRepository deviceRepository, JwtService jwtService) {
        this.deviceRepository = deviceRepository;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<Void> registerDevice(@RequestBody DeviceRegisterRequest request, HttpServletRequest httpRequest) {
        String token = resolveBearerToken(httpRequest);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Long userId = jwtService.extractClaim(token, claims -> {
            Object uid = claims.get("uid");
            if (uid == null) return null;
            return Long.valueOf(String.valueOf(uid));
        });

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        String deviceToken = request.getToken();
        if (deviceToken == null || deviceToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing device token");
        }

        Optional<DeviceEntity> existing = deviceRepository.findByUserIdAndToken(userId, deviceToken);
        DeviceEntity device = existing.orElseGet(DeviceEntity::new);
        device.setUserId(userId);
        device.setToken(deviceToken);
        device.setPlatform(request.getPlatform());
        deviceRepository.save(device);

        return ResponseEntity.noContent().build();
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
