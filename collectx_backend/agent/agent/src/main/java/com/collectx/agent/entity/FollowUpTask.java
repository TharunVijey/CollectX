package com.collectx.agent.entity;

import com.collectx.agent.enums.TaskStatus;
import com.collectx.agent.enums.TaskType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class FollowUpTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskId;

    private Long loanAccountId;

    private Long agentId;

    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    private TaskType taskType;

    private String priority;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;
}
