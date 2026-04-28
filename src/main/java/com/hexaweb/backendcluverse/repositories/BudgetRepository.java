package com.hexaweb.backendcluverse.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hexaweb.backendcluverse.entities.finance.Budget;
import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
	List<Budget> findByClubId(Long clubId);
	boolean existsByEventId(Long eventId);
	boolean existsByEventIdAndIdNot(Long eventId, Long id);
}
