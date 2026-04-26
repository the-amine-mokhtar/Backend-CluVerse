package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.finance.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
}

