package edu.cit.redoble.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.redoble.dto.auth.AuthResponse;
import edu.cit.redoble.entity.AuthProvider;
import edu.cit.redoble.entity.UserEntity;
import edu.cit.redoble.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final String oauth2SuccessRedirectUrl;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository,
                                              JwtService jwtService,
                                              ObjectMapper objectMapper,
                                              @Value("${app.oauth2.success-redirect-url:http://localhost:5173}") String oauth2SuccessRedirectUrl) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.oauth2SuccessRedirectUrl = oauth2SuccessRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String displayName = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");

        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google account email is not available");
            return;
        }

        NameParts nameParts = splitName(displayName);

        UserEntity user = userRepository.findByEmail(email.toLowerCase())
            .map(existing -> updateGoogleUser(existing, googleId, nameParts))
            .orElseGet(() -> createGoogleUser(email, nameParts, googleId));

        String token = jwtService.generateToken(user);
        AuthResponse authResponse = new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getProvider().name(),
                user.isStaff()
        );

        if (oauth2SuccessRedirectUrl == null || oauth2SuccessRedirectUrl.isBlank()) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), authResponse);
            return;
        }

        String redirectUrl = UriComponentsBuilder
                .fromUriString(oauth2SuccessRedirectUrl)
                .queryParam("token", authResponse.getToken())
                .queryParam("userId", authResponse.getUserId())
                .queryParam("email", authResponse.getEmail())
                .queryParam("firstName", authResponse.getFirstName())
                .queryParam("lastName", authResponse.getLastName())
                .queryParam("provider", authResponse.getProvider())
                .queryParam("isStaff", authResponse.isStaff())
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private UserEntity updateGoogleUser(UserEntity user, String googleId, NameParts nameParts) {
        if (user.getProvider() == AuthProvider.LOCAL) {
            return user;
        }

        user.setProvider(AuthProvider.GOOGLE);
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

    private record NameParts(String firstName, String lastName) {
    }
}
