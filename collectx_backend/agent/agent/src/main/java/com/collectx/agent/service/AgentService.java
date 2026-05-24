package com.collectx.agent.service;

import com.collectx.agent.dto.*;
import com.collectx.agent.entity.CaseNote;
import com.collectx.agent.entity.CollectionCase;
import com.collectx.agent.entity.FollowUpTask;
import com.collectx.agent.entity.HardshipFlag;
import com.collectx.agent.enums.CaseStatus;
import com.collectx.agent.enums.CaseType;
import com.collectx.agent.enums.GenericStatus;
import com.collectx.agent.enums.HardshipReason;
import com.collectx.agent.enums.Priority;
import com.collectx.agent.enums.TaskStatus;
import com.collectx.agent.enums.TaskType;
import com.collectx.agent.feign.PaymentClient;
import com.collectx.agent.repository.CaseRepository;
import com.collectx.agent.repository.HardshipRepository;
import com.collectx.agent.repository.NoteRepository;
import com.collectx.agent.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final CaseRepository caseRepo;
    private final NoteRepository noteRepo;
    private final TaskRepository taskRepo;
    private final HardshipRepository hardshipRepo;
    private final PaymentClient paymentClient;

    // ── PTP via Payment Service ────────────────────────────────────────────────

    public String createPTPFromAgent(AgentPTPRequestDTO dto, String token) {
        log.info("Agent creating PTP for loanAccountId={} amount={}", dto.getLoanAccountId(), dto.getAmount());
        Map<String, Object> body = new HashMap<>();
        body.put("loanAccountId", dto.getLoanAccountId());
        body.put("promisedAmount", dto.getAmount());
        body.put("promisedDate", LocalDate.now().plusDays(3).toString());
        body.put("promisedBy", "Agent");
        paymentClient.createPTP(body);
        log.info("PTP forwarded to payment service for loanAccountId={}", dto.getLoanAccountId());
        return "PTP Created";
    }

    // ── CASE ──────────────────────────────────────────────────────────────────

    public CaseResponseDTO createCase(CaseRequestDTO dto) {
        log.info("Creating collection case for loanAccountId={} type={}", dto.getLoanAccountId(), dto.getCaseType());
        CollectionCase c = new CollectionCase();
        c.setLoanAccountId(dto.getLoanAccountId());
        c.setCaseType(dto.getCaseType() != null ? CaseType.valueOf(dto.getCaseType()) : null);
        c.setPriority(dto.getPriority() != null ? Priority.valueOf(dto.getPriority()) : null);
        c.setOpenedDate(LocalDate.now());
        c.setStatus(CaseStatus.OPEN);
        CollectionCase saved = caseRepo.save(c);
        log.info("Collection case created with id={}", saved.getCaseId());
        return toCaseResponse(saved);
    }

    public List<CaseResponseDTO> getCases(Long loanId) {
        log.debug("Fetching cases for loanAccountId={}", loanId);
        return caseRepo.findByLoanAccountId(loanId).stream().map(this::toCaseResponse).collect(Collectors.toList());
    }

    // ── NOTE ──────────────────────────────────────────────────────────────────

    public NoteResponseDTO addNote(NoteRequestDTO dto) {
        log.info("Adding case note for loanAccountId={} agentId={}", dto.getLoanAccountId(), dto.getAgentId());
        CaseNote note = new CaseNote();
        note.setLoanAccountId(dto.getLoanAccountId());
        note.setAgentId(dto.getAgentId());
        note.setNote(dto.getNote());
        note.setNoteType(dto.getNoteType());
        note.setCreatedAt(java.time.LocalDateTime.now());
        CaseNote saved = noteRepo.save(note);
        log.info("Case note saved with id={}", saved.getNoteId());
        return toNoteResponse(saved);
    }

    public List<NoteResponseDTO> getNotesByLoan(Long loanAccountId) {
        log.debug("Fetching notes for loanAccountId={}", loanAccountId);
        return noteRepo.findByLoanAccountId(loanAccountId).stream().map(this::toNoteResponse).collect(Collectors.toList());
    }

    // ── TASK ──────────────────────────────────────────────────────────────────

    public TaskResponseDTO createTask(TaskRequestDTO dto) {
        log.info("Creating follow-up task for loanAccountId={} agentId={} type={}", dto.getLoanAccountId(), dto.getAgentId(), dto.getTaskType());
        FollowUpTask task = new FollowUpTask();
        task.setLoanAccountId(dto.getLoanAccountId());
        task.setAgentId(dto.getAgentId());
        task.setDueDate(dto.getDueDate() != null ? LocalDate.parse(dto.getDueDate()) : null);
        task.setTaskType(dto.getTaskType() != null ? TaskType.valueOf(dto.getTaskType()) : null);
        task.setPriority(dto.getPriority());
        task.setStatus(TaskStatus.OPEN);
        FollowUpTask saved = taskRepo.save(task);
        log.info("Follow-up task created with id={}", saved.getTaskId());
        return toTaskResponse(saved);
    }

    public List<TaskResponseDTO> getTasksByAgent(Long agentId) {
        log.debug("Fetching tasks for agentId={}", agentId);
        List<FollowUpTask> tasks = taskRepo.findByAgentId(agentId);
        // Auto-mark OVERDUE: if due date has passed and task is still OPEN, update to OVERDUE
        tasks.forEach(t -> {
            if (t.getStatus() == TaskStatus.OPEN && t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now())) {
                t.setStatus(TaskStatus.OVERDUE);
                taskRepo.save(t);
            }
        });
        return tasks.stream().map(this::toTaskResponse).collect(Collectors.toList());
    }

    public TaskResponseDTO updateTaskStatus(Long taskId, String status) {
        log.info("Updating task id={} to status={}", taskId, status);
        FollowUpTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        task.setStatus(TaskStatus.valueOf(status));
        return toTaskResponse(taskRepo.save(task));
    }

    // ── HARDSHIP FLAG ─────────────────────────────────────────────────────────

    public HardshipResponseDTO createHardship(HardshipRequestDTO dto) {
        log.info("Creating hardship flag for loanAccountId={} reason={}", dto.getLoanAccountId(), dto.getReason());
        HardshipFlag flag = new HardshipFlag();
        flag.setLoanAccountId(dto.getLoanAccountId());
        flag.setReason(dto.getReason() != null ? HardshipReason.valueOf(dto.getReason()) : null);
        flag.setStartDate(dto.getStartDate() != null ? LocalDate.parse(dto.getStartDate()) : LocalDate.now());
        flag.setEndDate(dto.getEndDate() != null ? LocalDate.parse(dto.getEndDate()) : null);
        flag.setStatus(GenericStatus.ACTIVE);
        HardshipFlag saved = hardshipRepo.save(flag);
        log.info("Hardship flag created with id={}", saved.getHardshipId());
        return toHardshipResponse(saved);
    }

    public List<HardshipResponseDTO> getHardshipsByLoan(Long loanAccountId) {
        log.debug("Fetching hardship flags for loanAccountId={}", loanAccountId);
        return hardshipRepo.findByLoanAccountId(loanAccountId).stream()
                .map(this::toHardshipResponse).collect(Collectors.toList());
    }

    public List<HardshipResponseDTO> getAllHardships() {
        log.debug("Fetching all hardship flags");
        return hardshipRepo.findAll().stream().map(this::toHardshipResponse).collect(Collectors.toList());
    }

    // ── MAPPERS ───────────────────────────────────────────────────────────────

    private CaseResponseDTO toCaseResponse(CollectionCase c) {
        return CaseResponseDTO.builder()
                .caseId(c.getCaseId())
                .loanAccountId(c.getLoanAccountId())
                .caseType(c.getCaseType() != null ? c.getCaseType().name() : null)
                .priority(c.getPriority() != null ? c.getPriority().name() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .openedDate(c.getOpenedDate() != null ? c.getOpenedDate().toString() : null)
                .build();
    }

    private NoteResponseDTO toNoteResponse(CaseNote n) {
        return NoteResponseDTO.builder()
                .noteId(n.getNoteId())
                .loanAccountId(n.getLoanAccountId())
                .agentId(n.getAgentId())
                .note(n.getNote())
                .noteType(n.getNoteType())
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
                .build();
    }

    private HardshipResponseDTO toHardshipResponse(HardshipFlag h) {
        return HardshipResponseDTO.builder()
                .hardshipId(h.getHardshipId())
                .loanAccountId(h.getLoanAccountId())
                .reason(h.getReason() != null ? h.getReason().name() : null)
                .startDate(h.getStartDate() != null ? h.getStartDate().toString() : null)
                .endDate(h.getEndDate() != null ? h.getEndDate().toString() : null)
                .status(h.getStatus() != null ? h.getStatus().name() : null)
                .build();
    }

    private TaskResponseDTO toTaskResponse(FollowUpTask t) {
        return TaskResponseDTO.builder()
                .taskId(t.getTaskId())
                .loanAccountId(t.getLoanAccountId())
                .agentId(t.getAgentId())
                .dueDate(t.getDueDate() != null ? t.getDueDate().toString() : null)
                .taskType(t.getTaskType() != null ? t.getTaskType().name() : null)
                .priority(t.getPriority())
                .status(t.getStatus() != null ? t.getStatus().name() : null)
                .build();
    }
}
