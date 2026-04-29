package com.hexaweb.backendcluverse.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hexaweb.backendcluverse.entities.finance.Transaction;
import com.hexaweb.backendcluverse.enumerations.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
	List<Transaction> findBySponsorId(Long sponsorId);
	List<Transaction> findBySponsorIdAndType(Long sponsorId, TransactionType type);
	List<Transaction> findBySponsorIdAndDateBetween(Long sponsorId, LocalDate startDate, LocalDate endDate);
	List<Transaction> findBySponsorIdAndTypeAndDateBetween(Long sponsorId, TransactionType type, LocalDate startDate, LocalDate endDate);
}
