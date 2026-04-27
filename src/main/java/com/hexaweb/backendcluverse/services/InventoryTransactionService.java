package com.hexaweb.backendcluverse.services;

import com.hexaweb.backendcluverse.dto.InventoryTransactionRequest;
import com.hexaweb.backendcluverse.entities.logistics.InventoryTransaction;
import com.hexaweb.backendcluverse.entities.logistics.Resource;
import com.hexaweb.backendcluverse.enumerations.InventoryTransactionType;
import com.hexaweb.backendcluverse.repositories.InventoryTransactionRepository;
import com.hexaweb.backendcluverse.repositories.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryTransactionService extends EntityServiceImpl<InventoryTransaction, Long> {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final ResourceRepository resourceRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final StockAlertService stockAlertService;

    public InventoryTransactionService(InventoryTransactionRepository repository,
                                      ResourceRepository resourceRepository,
                                      StockAlertService stockAlertService) {
        super(repository);
        this.transactionRepository = repository;
        this.resourceRepository = resourceRepository;
        this.stockAlertService = stockAlertService;
    }

    public List<InventoryTransaction> findByResourceId(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
            .orElseThrow(() -> new RuntimeException("Resource not found"));

        List<InventoryTransaction> transactions = transactionRepository.findByResourceIdOrderByDateAscIdAsc(resourceId);
        boolean stockChanged = false;

        for (InventoryTransaction tx : transactions) {
            if (tx.isApplied()) {
                continue;
            }

            InventoryTransactionType type = tx.getType();
            if (type != null && type != InventoryTransactionType.UPDATE) {
                int previousAvailable = resource.getAvailableQuantity();
                applyImpact(resource, type, tx.getQuantity());
                stockChanged = true;
                maybeSendLowStockAlert(resource, type, previousAvailable);
            }

            tx.setApplied(true);
            transactionRepository.save(tx);
        }

        if (stockChanged) {
            resourceRepository.save(resource);
        }

        return transactions;
    }

    public InventoryTransaction createTransaction(InventoryTransactionRequest req) {
        Resource r = resourceRepository.findById(req.getResourceId())
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        InventoryTransaction it = new InventoryTransaction();
        mapRequestToEntity(req, it);
        it.setResource(r);

        int previousAvailable = r.getAvailableQuantity();
        applyImpact(r, it.getType(), it.getQuantity());
        resourceRepository.save(r);
        maybeSendLowStockAlert(r, it.getType(), previousAvailable);

        it.setApplied(true);

        return save(it);
    }

    public InventoryTransaction updateTransaction(Long id, InventoryTransactionRequest req) {
        InventoryTransaction it = findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Resource previousResource = it.getResource();
        InventoryTransactionType previousType = it.getType();
        int previousQuantity = it.getQuantity();
        boolean previousApplied = it.isApplied();

        Resource nextResource = resourceRepository.findById(req.getResourceId())
            .orElseThrow(() -> new RuntimeException("Resource not found"));

        // Revert previous impact then apply next impact.
        if (previousApplied) {
            revertImpact(previousResource, previousType, previousQuantity);
        }

        int previousAvailable = nextResource.getAvailableQuantity();
        applyImpact(nextResource, req.getType(), req.getQuantity());
        maybeSendLowStockAlert(nextResource, req.getType(), previousAvailable);

        if (previousApplied) {
            resourceRepository.save(previousResource);
        }
        if (!previousResource.getId().equals(nextResource.getId())) {
            resourceRepository.save(nextResource);
        }

        mapRequestToEntity(req, it);
        it.setResource(nextResource);
        it.setApplied(true);
        return save(it);
    }

    public void deleteTransaction(Long id) {
        InventoryTransaction it = findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Resource r = it.getResource();
        if (it.isApplied()) {
            revertImpact(r, it.getType(), it.getQuantity());
            resourceRepository.save(r);
        }

        deleteById(id);
    }

    private void mapRequestToEntity(InventoryTransactionRequest req, InventoryTransaction it) {
        it.setType(req.getType());
        it.setQuantity(req.getQuantity());
        it.setDate(req.getDate());
        it.setReason(req.getReason());
    }

    private void maybeSendLowStockAlert(Resource resource, InventoryTransactionType type, int previousAvailableQuantity) {
        if (resource == null) {
            return;
        }

        if (type != InventoryTransactionType.REMOVE) {
            return;
        }

        int threshold = resource.getLowStockThreshold() > 0 ? resource.getLowStockThreshold() : LOW_STOCK_THRESHOLD;

        int currentAvailable = resource.getAvailableQuantity();
        if (currentAvailable >= threshold) {
            return;
        }

        stockAlertService.sendLowStockAlert(resource, threshold);
    }

    private void applyImpact(Resource resource, InventoryTransactionType type, int quantity) {
        if (type == null || type == InventoryTransactionType.UPDATE) {
            return;
        }
        if (quantity <= 0) {
            throw new RuntimeException("Quantity must be > 0");
        }

        int total = resource.getQuantityTotal();
        int available = resource.getAvailableQuantity();

        if (type == InventoryTransactionType.ADD) {
            total = total + quantity;
            available = available + quantity;
        } else if (type == InventoryTransactionType.REMOVE) {
            total = total - quantity;
            available = available - quantity;
        }

        if (total < 0 || available < 0) {
            throw new RuntimeException("Insufficient stock for this transaction");
        }

        if (available > total) {
            available = total;
        }

        resource.setQuantityTotal(total);
        resource.setAvailableQuantity(available);
    }

    private void revertImpact(Resource resource, InventoryTransactionType type, int quantity) {
        if (type == null || type == InventoryTransactionType.UPDATE) {
            return;
        }
        if (quantity <= 0) {
            return;
        }

        // Revert means apply the opposite.
        if (type == InventoryTransactionType.ADD) {
            applyImpact(resource, InventoryTransactionType.REMOVE, quantity);
        } else if (type == InventoryTransactionType.REMOVE) {
            applyImpact(resource, InventoryTransactionType.ADD, quantity);
        }
    }
}
