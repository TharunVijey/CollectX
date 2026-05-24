package com.collectx.iam.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;   // ★ NEW: needed for lockedUntil field

@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String name;
    private String email;
    private String password;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    // ★ NEW — Login Attempt Lockout fields
    // WHY: Track how many times a user has entered the wrong password.
    //      failedAttempts counts consecutive failures (resets to 0 on success).
    //      lockedUntil stores the DateTime until which the account is locked.
    //      Hibernate DDL-auto=update creates these columns automatically on restart.
    private int failedAttempts = 0;       // counts wrong password attempts (0 by default)
    private LocalDateTime lockedUntil;    // null = not locked; set to now+15min after 5 failures
}