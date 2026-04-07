package com.hexaweb.backendcluverse.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hexaweb.backendcluverse.entities.finance.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}

