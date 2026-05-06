package com.canals.homework.repository;

import com.canals.homework.model.InventoryItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

  @Modifying
  @Query(
      """
      UPDATE InventoryItem i
      SET i.quantity = i.quantity - :quantity
      WHERE i.warehouse.id = :warehouseId
      AND i.product.productId = :productId
      AND i.quantity >= :quantity
      """)
  int decreaseQuantity(
      @Param("warehouseId") UUID warehouseId,
      @Param("productId") UUID productId,
      @Param("quantity") Integer quantity);

  @Modifying
  @Query(
      """
      DELETE FROM InventoryItem i
      WHERE i.warehouse.id = :warehouseId
      AND i.product.productId = :productId
      AND i.quantity = 0
      """)
  void deleteIfEmpty(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId);
}
