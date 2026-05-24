package com.collectx.legal.repository;

import com.collectx.legal.entity.LegalAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegalRepository extends JpaRepository<LegalAction, Long> {
    List<LegalAction> findByLoanAccountId(Long loanAccountId);
}