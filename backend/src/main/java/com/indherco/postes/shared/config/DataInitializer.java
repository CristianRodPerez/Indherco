package com.indherco.postes.shared.config;

import com.indherco.postes.shared.enums.BaseRole;
import com.indherco.postes.users.User;
import com.indherco.postes.users.UserRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        @Value("${app.bootstrap.admin-username}") String adminUsername,
        @Value("${app.bootstrap.admin-password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername(adminUsername)) {
            log.info("Usuario administrador inicial ya existe: {}", adminUsername);
            return;
        }

        User admin = new User();
        admin.setName("Administrador");
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setBaseRole(BaseRole.ADMIN_OFICINA);
        admin.setCanRegisterProduction(true);
        admin.setCanRegisterDispatch(true);
        admin.setCanRegisterConsumption(true);
        admin.setLastPasswordChangeAt(LocalDateTime.now());
        userRepository.save(admin);
        log.info("Usuario administrador inicial creado: {}", adminUsername);
    }
}
