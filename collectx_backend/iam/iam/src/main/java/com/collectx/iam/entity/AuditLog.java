package com.collectx.iam.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    private String performedBy;
    private String action;
    private String targetEmail;
    private String details;

    private LocalDateTime createdAt;
}
