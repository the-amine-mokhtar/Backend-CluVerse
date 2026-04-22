package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.InventoryTransactionRequest;
import com.hexaweb.backendcluverse.entities.logistics.InventoryTransaction;
import com.hexaweb.backendcluverse.entities.logistics.Resource;
import com.hexaweb.backendcluverse.repositories.InventoryTransactionRepository;
import com.hexaweb.backendcluverse.repositories.ResourceRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryTransactionService extends EntityServiceImpl<InventoryTransaction, Long> {

    private final ResourceRepository resourceRepository;

    public InventoryTransactionService(InventoryTransactionRepository repository, ResourceRepository resourceRepository) {
        super(repository);
        this.resourceRepository = resourceRepository;
    }

    public InventoryTransaction createTransaction(InventoryTransactionRequest req) {
        Resource r = resourceRepository.findById(req.getResourceId())
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        InventoryTransaction it = new InventoryTransaction();
        mapRequestToEntity(req, it);
        it.setResource(r);
        return save(it);
    }

    public InventoryTransaction updateTransaction(Long id, InventoryTransactionRequest req) {
        InventoryTransaction it = findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        Resource r = resourceRepository.findById(req.getResourceId())
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        mapRequestToEntity(req, it);
        it.setResource(r);
        return save(it);
    }

    private void mapRequestToEntity(InventoryTransactionRequest req, InventoryTransaction it) {
        it.setType(req.getType());
        it.setQuantity(req.getQuantity());
        it.setDate(req.getDate());
        it.setReason(req.getReason());
    }
}
