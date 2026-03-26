package com.hexaweb.backendcluverse.controllers;

import com.hexaweb.backendcluverse.dto.InventoryTransactionRequest;
import com.hexaweb.backendcluverse.entities.logistics.InventoryTransaction;
import com.hexaweb.backendcluverse.services.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/inventory-transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private final InventoryTransactionService transactionService;

    @GetMapping
    public List<InventoryTransaction> getAll() {
        return transactionService.findAll();
    }

    @GetMapping("/{id}")
    public InventoryTransaction getById(@PathVariable Long id) {
        return transactionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public InventoryTransaction create(@RequestBody InventoryTransactionRequest request) {
        return transactionService.createTransaction(request);
    }

    @PutMapping("/{id}")
    public InventoryTransaction update(@PathVariable Long id, @RequestBody InventoryTransactionRequest request) {
        return transactionService.updateTransaction(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        transactionService.deleteById(id);
    }
}
