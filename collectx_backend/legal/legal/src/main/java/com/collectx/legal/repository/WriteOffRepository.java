package com.collectx.legal.repository;

import com.collectx.legal.entity.WriteOff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WriteOffRepository extends JpaRepository<WriteOff, Long> {
    Optional<WriteOff> findByLoanAccountId(Long loanAccountId);
}
