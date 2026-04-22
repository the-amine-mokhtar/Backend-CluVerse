package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.logistics.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
}
