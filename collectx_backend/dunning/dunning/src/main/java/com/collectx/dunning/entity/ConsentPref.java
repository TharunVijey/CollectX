package com.collectx.dunning.entity;

import com.collectx.dunning.entity.enums.Channel;
import com.collectx.dunning.entity.enums.ConsentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class ConsentPref {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long consentId;

    private Long customerId;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    private ConsentStatus status;

    private LocalDate updatedDate;
}
