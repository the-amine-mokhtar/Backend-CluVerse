package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.finance.Budget;
import com.hexaweb.backendcluverse.repositories.BudgetRepository;
import org.springframework.stereotype.Service;

@Service
public class BudgetService extends EntityServiceImpl<Budget, Long> {
    public BudgetService(BudgetRepository repository) {
        super(repository);
    }
}

