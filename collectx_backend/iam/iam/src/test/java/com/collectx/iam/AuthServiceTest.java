package com.collectx.iam;

import com.collectx.iam.dto.AuthResponseDTO;
import com.collectx.iam.dto.UserResponseDTO;
import com.collectx.iam.entity.Role;
import com.collectx.iam.entity.User;
import com.collectx.iam.entity.UserStatus;
import com.collectx.iam.repository.AuditLogRepository;
import com.collectx.iam.repository.UserRepository;
import com.collectx.iam.security.JwtUtil;
import com.collectx.iam.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        // This runs before each test — creates a fresh user every time
        sampleUser = new User();
        sampleUser.setUserId(1L);
        sampleUser.setName("John");
        sampleUser.setEmail("john@test.com");
        sampleUser.setPassword("$2a$10$hashedpassword"); // BCrypt format
        sampleUser.setRole(Role.AGENT);
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setFailedAttempts(0);
    }


    @Test
    void whenCorrectPassword_loginReturnsToken() {

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(true);
        when(jwtUtil.generateToken("john@test.com", "AGENT"))
                .thenReturn("mock.jwt.token");


        AuthResponseDTO response = authService.login("john@test.com", "password123");

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("AGENT", response.getRole());
    }

    @Test
    void whenWrongPassword_failedAttemptsIncreases() {

        // Arrange
        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenReturn(sampleUser);
        when(auditLogRepository.save(any()))
                .thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> authService.login("john@test.com", "wrongpassword"));

        assertEquals(1, sampleUser.getFailedAttempts());
    }


    @Test
    void getAllUsers_returnsListWithCorrectDetails() {

        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserResponseDTO> result = authService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("john@test.com", result.get(0).getEmail());
        assertEquals("AGENT", result.get(0).getRole());
    }
}
