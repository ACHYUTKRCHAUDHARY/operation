package com.achyut.operation.config;

import com.achyut.operation.auth.AppUser;
import com.achyut.operation.auth.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthBootstrapConfig {
    @Bean
    CommandLineRunner seedAdmin(AppUserRepository users, PasswordEncoder encoder,
                                @Value("${app.auth.bootstrap-admin-email:admin@operation.local}") String email,
                                @Value("${app.auth.bootstrap-admin-password:Admin@12345}") String password) {
        return args -> {
            if (!users.existsByEmailIgnoreCase(email)) {
                users.save(AppUser.builder().email(email).passwordHash(encoder.encode(password))
                    .fullName("Operations Admin").role(AppUser.Role.ADMIN).enabled(true).build());
            }
        };
    }
}
