package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}

