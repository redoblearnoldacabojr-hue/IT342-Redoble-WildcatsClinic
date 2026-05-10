package edu.cit.redoble.features.auth.service;

import edu.cit.redoble.features.auth.entity.UserEntity;
import edu.cit.redoble.features.auth.repository.UserRepository;
import edu.cit.redoble.features.shared.util.AdminAuthorizationService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public CustomUserDetailsService(UserRepository userRepository,
                                    AdminAuthorizationService adminAuthorizationService) {
        this.userRepository = userRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedEmail));

        String password = user.getPassword() == null ? "" : user.getPassword();

        User.UserBuilder builder = User.withUsername(user.getEmail())
                .password(password);

        if (adminAuthorizationService.isAdminEmail(user.getEmail())) {
            builder.roles("USER", "ADMIN");
        } else {
            builder.roles("USER");
        }

        return builder.build();
    }
}
