package com.collectx.agent.entity;

import com.collectx.agent.enums.CaseStatus;
import com.collectx.agent.enums.CaseType;
import com.collectx.agent.enums.Priority;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class CollectionCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long caseId;

    private Long loanAccountId;

    private LocalDate openedDate;

    @Enumerated(EnumType.STRING)
    private CaseType caseType;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private CaseStatus status;
}