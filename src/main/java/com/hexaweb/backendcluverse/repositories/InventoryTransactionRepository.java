package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.logistics.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

	List<InventoryTransaction> findByResourceId(Long resourceId);

	List<InventoryTransaction> findByResourceIdOrderByDateAscIdAsc(Long resourceId);
}
