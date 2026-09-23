package com.son.blog.config;

import com.son.blog.entity.User;
import com.son.blog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("superadmin")) {
            User superAdmin = User.builder()
                    .username("superadmin")
                    .password(passwordEncoder.encode("superadmin123"))
                    .email("superadmin@example.com")
                    .role("ROLE_SUPER_ADMIN")
                    .build();
            userRepository.save(superAdmin);
            System.out.println("Default Super Admin created: superadmin / superadmin123");
        }
    }
}
