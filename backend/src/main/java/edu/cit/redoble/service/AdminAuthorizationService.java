package edu.cit.redoble.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminAuthorizationService {

    private final Set<String> adminEmails;

    public AdminAuthorizationService(@Value("${app.admin.emails:}") String adminEmailsConfig) {
        this.adminEmails = Arrays.stream(adminEmailsConfig.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(email -> !email.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdminEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        return adminEmails.contains(email.trim().toLowerCase());
    }
}
