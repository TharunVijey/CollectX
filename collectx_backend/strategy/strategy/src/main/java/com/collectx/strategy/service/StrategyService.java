package com.collectx.strategy.service;

import com.collectx.strategy.dto.AssignmentResponseDTO;
import com.collectx.strategy.dto.QueueRequestDTO;
import com.collectx.strategy.dto.QueueResponseDTO;
import com.collectx.strategy.dto.RuleRequestDTO;
import com.collectx.strategy.dto.RuleResponseDTO;
import com.collectx.strategy.entity.Assignment;
import com.collectx.strategy.entity.Queue;
import com.collectx.strategy.entity.StrategyRule;
import com.collectx.strategy.feign.IamClient;
import com.collectx.strategy.feign.NotificationClient;
import com.collectx.strategy.repository.AssignmentRepository;
import com.collectx.strategy.repository.QueueRepository;
import com.collectx.strategy.repository.StrategyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StrategyService {

    private static final Logger log = LoggerFactory.getLogger(StrategyService.class);

    private final StrategyRepository strategyRepo;
    private final AssignmentRepository assignmentRepo;
    private final QueueRepository queueRepo;
    private final NotificationClient notificationClient;
    private final IamClient iamClient;

    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    private final AtomicInteger queueRoundRobinCounter = new AtomicInteger(0);

    // ── ASSIGN LOAN ───────────────────────────────────────────────────────────

    public Assignment assignLoan(Long loanAccountId,
                                 String bucket,
                                 String riskBand,
                                 String token) {

        log.info("Assigning loan={} bucket={} riskBand={}", loanAccountId, bucket, riskBand);

        List<StrategyRule> rules = strategyRepo.findByStatusOrderByPriorityAsc("ACTIVE");

        Assignment assignment = new Assignment();
        assignment.setLoanAccountId(loanAccountId);
        assignment.setQueueId(pickQueue(bucket, riskBand));
        assignment.setAssignedDate(LocalDate.now());
        assignment.setStatus("OPEN");

        // Try to find a matching strategy rule
        boolean ruleMatched = false;
        for (StrategyRule rule : rules) {
            if (rule.getBucket().equals(bucket) && rule.getRiskBand().equals(riskBand)) {
                assignment.setAgentId(pickNextAgent());
                ruleMatched = true;
                log.info("Matched strategy rule id={} for loan={}", rule.getRuleId(), loanAccountId);
                break;
            }
        }

        // If no matching rule found, still use round-robin from real agents
        if (!ruleMatched) {
            assignment.setAgentId(pickNextAgent());
            log.warn("No matching strategy rule for bucket={} riskBand={} — using round-robin fallback", bucket, riskBand);
        }

        Assignment saved = assignmentRepo.save(assignment);
        log.info("Assignment created with id={} agentId={}", saved.getAssignmentId(), saved.getAgentId());

        // Send notification to the assigned agent
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("customerId", 0);
            body.put("loanAccountId", loanAccountId);
            body.put("message", "New loan #L" + loanAccountId + " assigned to you (Bucket: " + bucket + ")");
            body.put("channel", "INAPP");
            body.put("notificationType", "SYSTEM");
            notificationClient.sendNotification(body);
        } catch (Exception e) {
            log.warn("Notification failed for loan assignment loan={} — {}", loanAccountId, e.getMessage());
        }

        return saved;
    }

    // ── CREATE RULE ───────────────────────────────────────────────────────────

    public RuleResponseDTO createRule(RuleRequestDTO dto) {
        log.info("Creating strategy rule name={} bucket={} riskBand={} priority={}", dto.getName(), dto.getBucket(), dto.getRiskBand(), dto.getPriority());

        StrategyRule rule = new StrategyRule();
        // ruleId intentionally NOT set — @GeneratedValue(IDENTITY) handles it
        rule.setName(dto.getName());
        rule.setBucket(dto.getBucket());
        rule.setRiskBand(dto.getRiskBand());
        rule.setPriority(dto.getPriority());
        rule.setStatus("ACTIVE");

        StrategyRule saved = strategyRepo.save(rule);
        log.info("Strategy rule created with id={}", saved.getRuleId());
        return toRuleResponse(saved);
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    public List<AssignmentResponseDTO> getAssignments() {
        log.debug("Fetching all assignments");
        return assignmentRepo.findAll().stream().map(this::toAssignmentResponse).collect(Collectors.toList());
    }

    public List<RuleResponseDTO> getRules() {
        log.debug("Fetching all strategy rules");
        return strategyRepo.findAll().stream().map(this::toRuleResponse).collect(Collectors.toList());
    }

    // ── QUEUE MANAGEMENT ──────────────────────────────────────────────────────

    public QueueResponseDTO createQueue(QueueRequestDTO dto) {
        log.info("Creating queue name={} bucketScope={}", dto.getName(), dto.getBucketScope());
        Queue q = new Queue();
        q.setName(dto.getName());
        q.setBucketScope(dto.getBucketScope());
        q.setRiskBand(dto.getRiskBand());
        q.setTerritory(dto.getTerritory());
        q.setCapacity(dto.getCapacity());
        q.setStatus("ACTIVE");
        Queue saved = queueRepo.save(q);
        log.info("Queue created with id={}", saved.getQueueId());
        return toQueueResponse(saved);
    }

    public List<QueueResponseDTO> getQueues() {
        log.debug("Fetching all queues");
        return queueRepo.findAll().stream().map(this::toQueueResponse).collect(Collectors.toList());
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /**
     * Picks the next agent using round-robin from real AGENT users in IAM.
     * Falls back to agent ID 1 if IAM is unreachable or has no agents.
     */
    private Long pickNextAgent() {
        try {
            List<Long> agents = iamClient.getAgentIds();
            if (agents != null && !agents.isEmpty()) {
                int idx = Math.abs(roundRobinCounter.getAndIncrement() % agents.size());
                Long picked = agents.get(idx);
                log.info("Round-robin agent assignment: picked agentId={} (index={} of {})", picked, idx, agents.size());
                return picked;
            }
        } catch (Exception e) {
            log.warn("Could not fetch agents from IAM — defaulting to agent 1. Reason: {}", e.getMessage());
        }
        return 1L;
    }

    /**
     * Picks a queue using round-robin from all ACTIVE queues matching the bucket.
     * Falls back to queue ID 1 if no matching queue is found.
     */
    private Long pickQueue(String bucket, String riskBand) {
        List<Queue> matchingQueues = queueRepo.findAll()
                .stream()
                .filter(q -> "ACTIVE".equals(q.getStatus())
                          && bucket.equals(q.getBucketScope()))
                .collect(Collectors.toList());

        if (!matchingQueues.isEmpty()) {
            int idx = Math.abs(queueRoundRobinCounter.getAndIncrement() % matchingQueues.size());
            Long picked = matchingQueues.get(idx).getQueueId();
            log.info("Queue selected: queueId={} bucket={} riskBand={}", picked, bucket, riskBand);
            return picked;
        }

        log.warn("No active queue found for bucket={} riskBand={} — defaulting to queue 1", bucket, riskBand);
        return 1L;
    }

    // ── MAPPERS ───────────────────────────────────────────────────────────────

    private AssignmentResponseDTO toAssignmentResponse(Assignment a) {
        return AssignmentResponseDTO.builder()
                .assignmentId(a.getAssignmentId())
                .loanAccountId(a.getLoanAccountId())
                .agentId(a.getAgentId())
                .queueId(a.getQueueId())
                .assignedDate(a.getAssignedDate() != null ? a.getAssignedDate().toString() : null)
                .status(a.getStatus())
                .build();
    }

    private QueueResponseDTO toQueueResponse(Queue q) {
        return QueueResponseDTO.builder()
                .queueId(q.getQueueId())
                .name(q.getName())
                .bucketScope(q.getBucketScope())
                .riskBand(q.getRiskBand())
                .territory(q.getTerritory())
                .capacity(q.getCapacity())
                .status(q.getStatus())
                .build();
    }

    private RuleResponseDTO toRuleResponse(StrategyRule r) {
        return RuleResponseDTO.builder()
                .ruleId(r.getRuleId())
                .name(r.getName())
                .bucket(r.getBucket())
                .riskBand(r.getRiskBand())
                .priority(r.getPriority())
                .status(r.getStatus())
                .build();
    }
}
