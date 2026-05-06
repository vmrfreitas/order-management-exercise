package com.canals.homework.repository;

import com.canals.homework.model.Warehouse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

  @Query(
      """
      SELECT w FROM Warehouse w
      WHERE NOT EXISTS (
        SELECT 1 FROM OrderItem oi
        WHERE NOT EXISTS (
          SELECT 1 FROM InventoryItem i
          WHERE i.warehouse = w
          AND i.product = oi.product
          AND i.quantity >= oi.quantity
        )
        AND oi.order.id = :orderId
      )
      """)
  List<Warehouse> findWarehousesWithAllItems(@Param("orderId") UUID orderId);
}
