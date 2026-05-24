package com.collectx.payment.repository;

import com.collectx.payment.entity.PTP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PTPRepository extends JpaRepository<PTP, Long> {
    List<PTP> findByLoanAccountId(Long loanId);
}