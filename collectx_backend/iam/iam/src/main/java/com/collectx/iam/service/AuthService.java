package com.collectx.iam.service;

import com.collectx.iam.dto.AuthResponseDTO;
import com.collectx.iam.dto.RegisterRequestDTO;
import com.collectx.iam.dto.UpdateUserRequestDTO;
import com.collectx.iam.dto.UserResponseDTO;
import com.collectx.iam.entity.AuditLog;
import com.collectx.iam.entity.Role;
import com.collectx.iam.entity.User;
import com.collectx.iam.entity.UserStatus;
import com.collectx.iam.exception.AccountLockedException;
import com.collectx.iam.exception.EmailAlreadyExistsException;
import com.collectx.iam.exception.InvalidCredentialsException;
import com.collectx.iam.exception.UserNotFoundException;
import com.collectx.iam.repository.AuditLogRepository;
import com.collectx.iam.repository.UserRepository;
import com.collectx.iam.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    private static final long EXPIRES_IN_SECONDS   = 60 * 60 * 10;
    private static final int  MAX_FAILED_ATTEMPTS   = 5;
    private static final int  LOCK_DURATION_MINUTES = 15;

    @Transactional
    public UserResponseDTO createUser(RegisterRequestDTO dto) {
        log.info("Admin creating user email={} role={}", dto.getEmail(), dto.getRole());
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
        }
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setStatus(UserStatus.ACTIVE);
        user.setPhone(dto.getPhone());
        UserResponseDTO result = toDTO(userRepository.save(user));
        saveAudit("system", "CREATE_USER", dto.getEmail(), "Created user role=" + dto.getRole());
        return result;
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDTO updateUser(Long userId, UpdateUserRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        if (dto.getName()   != null) user.setName(dto.getName());
        if (dto.getEmail()  != null) {
            if (!dto.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
                throw new EmailAlreadyExistsException("Email already exists: " + dto.getEmail());
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getRole()   != null) user.setRole(dto.getRole());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        UserResponseDTO updated = toDTO(userRepository.save(user));
        saveAudit("system", "UPDATE_USER", user.getEmail(), "Updated user id=" + userId);
        return updated;
    }

    @Transactional
    public String deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        String email = user.getEmail();
        userRepository.delete(user);
        saveAudit("system", "DELETE_USER", email, "Deleted user id=" + userId);
        return "User deleted";
    }

    private UserResponseDTO toDTO(User user) {
        return new UserResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getPhone()
        );
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    private void saveAudit(String performedBy, String action, String targetEmail, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setPerformedBy(performedBy);
            entry.setAction(action);
            entry.setTargetEmail(targetEmail);
            entry.setDetails(details);
            entry.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // audit failure must never break the main operation
            // but log as error so monitoring tools can alert on it
            log.error("AUDIT LOG FAILURE — action={} target={} reason={}", action, targetEmail, e.getMessage());
        }
    }

    public List<Long> getAgentIds() {
        return userRepository.findByRole(Role.AGENT)
                .stream()
                .map(User::getUserId)
                .collect(Collectors.toList());
    }

    @Transactional
    public AuthResponseDTO login(String email, String password) {
        log.info("Login attempt for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if account is locked
        if (user.getLockedUntil() != null) {
            if (LocalDateTime.now().isBefore(user.getLockedUntil())) {
                long minutesLeft = java.time.Duration.between(LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
                saveAudit(email, "LOGIN_BLOCKED", email, "Account locked — " + minutesLeft + " min remaining");
                throw new AccountLockedException("Account is locked. Try again in " + minutesLeft + " minute(s).");
            } else {
                // Lock has expired — auto-unlock
                user.setLockedUntil(null);
                user.setFailedAttempts(0);
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
                log.info("Account auto-unlocked for email={}", email);
            }
        }

        // Verify password — BCrypt only (plain text legacy removed)
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());

        if (!passwordMatches) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                user.setStatus(UserStatus.LOCKED);
                userRepository.save(user);
                saveAudit("system", "ACCOUNT_LOCKED", email,
                        "Locked after " + MAX_FAILED_ATTEMPTS + " failed attempts");
                log.warn("Account LOCKED for email={} after {} failed attempts", email, MAX_FAILED_ATTEMPTS);
                throw new AccountLockedException(
                        "Account locked after " + MAX_FAILED_ATTEMPTS + " failed attempts. Try again in " + LOCK_DURATION_MINUTES + " minutes.");
            } else {
                userRepository.save(user);
                saveAudit("system", "LOGIN_FAILED", email,
                        "Invalid password — attempt " + attempts + "/" + MAX_FAILED_ATTEMPTS);
                log.warn("Failed login attempt {}/{} for email={}", attempts, MAX_FAILED_ATTEMPTS, email);
                throw new InvalidCredentialsException(
                        "Invalid credentials. " + (MAX_FAILED_ATTEMPTS - attempts) + " attempt(s) remaining before lockout.");
            }
        }

        // Successful login — reset failure tracking
        if (user.getFailedAttempts() > 0 || user.getStatus() != UserStatus.ACTIVE) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        saveAudit(email, "LOGIN_SUCCESS", email, "Successful login");

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponseDTO(token, user.getEmail(), user.getRole().name(), EXPIRES_IN_SECONDS);
    }
}
