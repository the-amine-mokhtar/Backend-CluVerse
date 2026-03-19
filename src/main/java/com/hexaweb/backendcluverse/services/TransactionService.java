package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.entities.finance.Transaction;
import com.hexaweb.backendcluverse.repositories.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionService extends EntityServiceImpl<Transaction, Long> {
    public TransactionService(TransactionRepository repository) {
        super(repository);
    }
}

