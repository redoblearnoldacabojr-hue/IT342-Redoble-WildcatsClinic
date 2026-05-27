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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import edu.cit.redoble.features.auth.dto.GoogleSignInRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String MOBILE_OAUTH_REDIRECT_SESSION_ATTRIBUTE = "mobile_oauth_redirect_uri";
    private static final String MOBILE_OAUTH_REDIRECT_URI = "wildcatclinic://auth/google";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ID = "id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME = "lastName";
    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_ROLE = "role";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String googleClientId;

    public AuthController(AuthService authService, UserRepository userRepository, JwtService jwtService,
                          @Value("${spring.security.oauth2.client.registration.google.client-id:}") String googleClientId) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.googleClientId = googleClientId;
    }

    @GetMapping("/google/start")
    public void startGoogleSignIn(@RequestParam(value = "redirect_uri", required = false) String redirectUri,
                                  HttpServletRequest request,
                                  HttpServletResponse response) throws java.io.IOException {
        if (redirectUri != null && !redirectUri.isBlank()) {
            String normalizedRedirectUri = redirectUri.trim();
            if (!MOBILE_OAUTH_REDIRECT_URI.equals(normalizedRedirectUri)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid redirect_uri");
            }

            HttpSession session = request.getSession(true);
            session.setAttribute(MOBILE_OAUTH_REDIRECT_SESSION_ATTRIBUTE, normalizedRedirectUri);
            // Also set a short-lived cookie so browsers that don't preserve the server session
            // across the external Google authorization round-trip can still be identified.
            javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie("mobile_oauth", "1");
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            // Do not mark Secure here; server may be used in local dev. In production HTTPS should be used.
            cookie.setMaxAge(300); // 5 minutes
            response.addCookie(cookie);
        }

        response.sendRedirect("/oauth2/authorization/google");
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleSignIn(@RequestBody GoogleSignInRequest request) {
        String idTokenString = request == null ? null : request.getIdToken();
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing idToken");
        }

        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            var jsonFactory = GsonFactory.getDefaultInstance();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .setAudience(googleClientId == null || googleClientId.isBlank()
                        ? Collections.emptyList()
                        : Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String sub = payload.getSubject();

            AuthResponse response = authService.loginWithGooglePayload(email, name, sub);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to verify ID token", ex);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request) {
        UserEntity user = resolveCurrentUser(authentication, request);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.noContent().build();
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
                    KEY_USER_ID, user.getId(),
                    KEY_ID, user.getId(),
                    KEY_EMAIL, user.getEmail(),
                    KEY_FIRST_NAME, user.getFirstName(),
                    KEY_LAST_NAME, user.getLastName(),
                    KEY_PROVIDER, user.getProvider().name(),
                    KEY_ROLE, user.getRole()
            ));
        }

        String token = resolveBearerToken(request);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        Map<String, Object> claims = jwtService.extractClaim(token, claimsMap -> new HashMap<>(claimsMap));
        Object uid = claims.get("uid");
        Object firstName = claims.getOrDefault(KEY_FIRST_NAME, "");
        Object lastName = claims.getOrDefault(KEY_LAST_NAME, "");
        Object provider = claims.getOrDefault(KEY_PROVIDER, "LOCAL");
        Object role = claims.getOrDefault(KEY_ROLE, 1);

        return ResponseEntity.ok(Map.of(
            KEY_USER_ID, uid == null ? null : Long.valueOf(String.valueOf(uid)),
                KEY_ID, uid == null ? null : Long.valueOf(String.valueOf(uid)),
                KEY_EMAIL, email,
                KEY_FIRST_NAME, String.valueOf(firstName),
                KEY_LAST_NAME, String.valueOf(lastName),
                KEY_PROVIDER, String.valueOf(provider),
            KEY_ROLE, Integer.parseInt(String.valueOf(role))
        ));
    }

    private UserEntity resolveCurrentUser(Authentication authentication, HttpServletRequest request) {
        String email = resolveEmail(authentication, request);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        return userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private String resolveEmail(Authentication authentication, HttpServletRequest request) {
        String token = resolveBearerToken(request);
        if (token != null && !token.isBlank()) {
            try {
                return jwtService.extractUsername(token).toLowerCase();
            } catch (Exception ex) {
                return null;
            }
        }

        if (authentication != null && authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute(KEY_EMAIL);
            if (email != null && !email.isBlank()) {
                return email.trim().toLowerCase();
            }
        }

        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank() && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName().trim().toLowerCase();
        }

        return null;
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
