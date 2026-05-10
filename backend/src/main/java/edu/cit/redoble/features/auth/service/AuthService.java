package edu.cit.redoble.features.auth.service;

import edu.cit.redoble.features.auth.dto.AuthResponse;
import edu.cit.redoble.features.auth.dto.LoginRequest;
import edu.cit.redoble.features.auth.dto.RegisterRequest;
import edu.cit.redoble.features.auth.entity.AuthProvider;
import edu.cit.redoble.features.auth.entity.UserEntity;
import edu.cit.redoble.features.auth.repository.UserRepository;
import edu.cit.redoble.features.shared.security.JwtService;
import edu.cit.redoble.features.shared.util.AdminAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AdminAuthorizationService adminAuthorizationService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AdminAuthorizationService adminAuthorizationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProvider(AuthProvider.LOCAL);
        user.setStaff(false);

        UserEntity savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        return toAuthResponse(savedUser, token);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This account uses " + user.getProvider().name().toLowerCase() + " login");
        }

        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    public AuthResponse loginWithGoogle(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        String displayName = oauth2User.getAttribute("name");
        String googleId = oauth2User.getAttribute("sub");

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account email is not available");
        }

        NameParts nameParts = splitName(displayName);

        UserEntity user = userRepository.findByEmail(email.toLowerCase())
            .map(existing -> updateGoogleData(existing, googleId, nameParts))
            .orElseGet(() -> createGoogleUser(email, nameParts, googleId));

        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    public void updateStaffStatus(Long targetUserId, boolean staffStatus, String requesterEmail) {
        if (!adminAuthorizationService.isAdminEmail(requesterEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        UserEntity targetUser = userRepository.findByEmail(String.valueOf(targetUserId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        targetUser.setStaff(staffStatus);
        userRepository.save(targetUser);
    }

    private UserEntity updateGoogleData(UserEntity user, String googleId, NameParts nameParts) {
        if (user.getProvider() != AuthProvider.GOOGLE) {
            user.setProvider(AuthProvider.GOOGLE);
        }
        user.setProviderId(googleId);
        user.setFirstName(nameParts.firstName());
        user.setLastName(nameParts.lastName());
        return userRepository.save(user);
    }

    private UserEntity createGoogleUser(String email, NameParts nameParts, String googleId) {
        UserEntity user = new UserEntity();
        user.setEmail(email.trim().toLowerCase());
        user.setFirstName(nameParts.firstName());
        user.setLastName(nameParts.lastName());
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(googleId);
        user.setStaff(false);
        return userRepository.save(user);
    }

    private NameParts splitName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return new NameParts("Google", "User");
        }

        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            return new NameParts(parts[0], "User");
        }

        String firstName = parts[0];
        String lastName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        return new NameParts(firstName, lastName);
    }

    private AuthResponse toAuthResponse(UserEntity user, String token) {
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getProvider().name(),
                user.isStaff()
        );
    }

    private record NameParts(String firstName, String lastName) {
    }
}
