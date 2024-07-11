package com.cesar.authservice;

import com.cesar.authservice.entity.AuthUser;
import com.cesar.authservice.entity.Role;
import com.cesar.authservice.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class AuthServiceApplication implements CommandLineRunner {

    private final AuthUserRepository authUserRepository;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    public AuthServiceApplication(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    // This will create an admin user if it does not exist, you can change all these presets to environment variables
    @Override
    public void run(String... args) throws Exception {
        if (authUserRepository.findByRole(Role.ADMIN) == null) {
            authUserRepository.save(AuthUser.builder()
                            .email("admin@mail.com")
                            .password(new BCryptPasswordEncoder().encode(adminPassword))
                            .firstName("ADMIN")
                            .lastName("ADMIN")
                            .role(Role.ADMIN)
                            .build());
        }
    }
}
