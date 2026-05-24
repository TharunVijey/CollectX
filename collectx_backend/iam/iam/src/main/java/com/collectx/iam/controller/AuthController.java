package com.collectx.iam.controller;

import com.collectx.iam.dto.AuthResponseDTO;
import com.collectx.iam.dto.LoginRequestDTO;
import com.collectx.iam.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) {
        log.info("Login request received for email={}", dto.getEmail());
        return authService.login(dto.getEmail(), dto.getPassword());
    }

    /**
     * Internal endpoint: returns IDs of all users with AGENT role.
     * Used by strategy-service for round-robin loan assignment.
     * Permitted to all (no JWT needed) because it's an internal service call.
     */
    @GetMapping("/agents")
    public List<Long> getAgentIds() {
        log.info("Agent IDs requested");
        return authService.getAgentIds();
    }

}
