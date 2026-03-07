package edu.cit.redoble.service;

import edu.cit.redoble.entity.UserEntity;
import edu.cit.redoble.repository.UserRepository;
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
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

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
