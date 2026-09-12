package com.mitti2market.controller;

import com.mitti2market.dto.ApiResponse;
import com.mitti2market.dto.warehouse.WarehouseRequest;
import com.mitti2market.dto.warehouse.WarehouseResponse;
import com.mitti2market.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    /** List all active warehouses / regional hubs */
    @GetMapping
    public ResponseEntity<?> getActiveWarehouses() {
        List<WarehouseResponse> list = warehouseService.getAllActive();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /** Find nearest warehouses to coordinates */
    @GetMapping("/nearest")
    public ResponseEntity<?> getNearestWarehouses(
            @RequestParam double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double lon) {
        double effectiveLng = lng != null ? lng : (lon != null ? lon : 0.0);
        List<WarehouseResponse> list = warehouseService.findNearestTo(lat, effectiveLng);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /** Get warehouse by ID */
    @GetMapping("/{id}")
    public ResponseEntity<?> getWarehouseById(@PathVariable Long id) {
        try {
            WarehouseResponse res = warehouseService.getById(id);
            return ResponseEntity.ok(ApiResponse.ok(res));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Create warehouse (Admin only) */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<?> createWarehouse(@RequestBody WarehouseRequest request) {
        try {
            WarehouseResponse res = warehouseService.createWarehouse(request);
            return ResponseEntity.ok(ApiResponse.ok("Warehouse created", res));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Update warehouse (Admin only) */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWarehouse(
            @PathVariable Long id,
            @RequestBody WarehouseRequest request) {
        try {
            WarehouseResponse res = warehouseService.updateWarehouse(id, request);
            return ResponseEntity.ok(ApiResponse.ok("Warehouse updated", res));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
