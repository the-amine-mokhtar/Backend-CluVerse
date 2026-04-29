package com.hexaweb.backendcluverse.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hexaweb.backendcluverse.entities.finance.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
	boolean existsByEventId(Long eventId);
	boolean existsByEventIdAndIdNot(Long eventId, Long id);
}
