package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.entities.finance.Budget;
import com.hexaweb.backendcluverse.services.BudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public List<Budget> getAll() {
        return budgetService.findAll();
    }

    @GetMapping("/{id}")
    public Budget getById(@PathVariable Long id) {
        return budgetService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Budget create(@RequestBody Budget budget) {
        return budgetService.save(budget);
    }

    @PutMapping("/{id}")
    public Budget update(@PathVariable Long id, @RequestBody Budget budget) {
        budget.setId(id);
        return budgetService.save(budget);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        budgetService.deleteById(id);
    }
}

