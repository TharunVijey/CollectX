package com.collectx.iam.controller;

import com.collectx.iam.dto.RegisterRequestDTO;
import com.collectx.iam.dto.UpdateUserRequestDTO;
import com.collectx.iam.dto.UserResponseDTO;
import com.collectx.iam.entity.AuditLog;
import com.collectx.iam.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AuthService authService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public UserResponseDTO createUser(@Valid @RequestBody RegisterRequestDTO dto) {
        log.info("Admin creating user email={} role={}", dto.getEmail(), dto.getRole());
        return authService.createUser(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers() {
        log.info("Admin fetching all users");
        return authService.getAllUsers();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}")
    public UserResponseDTO updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequestDTO dto) {
        log.info("Admin updating user id={}", id);
        return authService.updateUser(id, dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable Long id) {
        log.info("Admin deleting user id={}", id);
        return authService.deleteUser(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit-logs")
    public List<AuditLog> getAuditLogs() {
        log.info("Admin fetching audit logs");
        return authService.getAllAuditLogs();
    }
}
