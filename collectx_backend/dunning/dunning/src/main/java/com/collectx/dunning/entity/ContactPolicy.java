package com.collectx.dunning.entity;

import com.collectx.dunning.entity.enums.YesNoFlag;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ContactPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    private String bucket;

    private Integer maxAttemptsPerDay;
    private Integer minGapMinutes;

    private String preferredChannels; // keep as JSON for now

    @Enumerated(EnumType.STRING)
    private YesNoFlag doNotCallFlag;
}