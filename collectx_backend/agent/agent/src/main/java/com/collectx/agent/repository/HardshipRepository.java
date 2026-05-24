package com.collectx.agent.repository;

import com.collectx.agent.entity.HardshipFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HardshipRepository extends JpaRepository<HardshipFlag, Long> {
    List<HardshipFlag> findByLoanAccountId(Long loanAccountId);
}