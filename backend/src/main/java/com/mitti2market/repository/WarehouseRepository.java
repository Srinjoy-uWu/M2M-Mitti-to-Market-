package com.mitti2market.repository;

import com.mitti2market.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByActiveTrue();

    List<Warehouse> findAllByOrderByNameAsc();
}
