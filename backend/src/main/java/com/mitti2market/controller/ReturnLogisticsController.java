package com.mitti2market.controller;

import com.mitti2market.dto.ApiResponse;
import com.mitti2market.dto.returns.ReturnShipmentRequest;
import com.mitti2market.dto.returns.ReturnShipmentResponse;
import com.mitti2market.model.ReturnShipment;
import com.mitti2market.service.ReturnLogisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class ReturnLogisticsController {

    private final ReturnLogisticsService returnService;

    /** Create a return shipment for a deal */
    @PostMapping("/{dealId}/returns")
    public ResponseEntity<?> createReturn(
            @PathVariable Long dealId,
            @RequestBody ReturnShipmentRequest request) {
        try {
            ReturnShipment ret = returnService.createManualReturn(dealId, request);
            return ResponseEntity.ok(ApiResponse.ok("Return shipment requested", ReturnShipmentResponse.fromEntity(ret)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** List all return shipments for a deal */
    @GetMapping("/{dealId}/returns")
    public ResponseEntity<?> getReturnsForDeal(@PathVariable Long dealId) {
        List<ReturnShipmentResponse> list = returnService.getReturnsForDeal(dealId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /** Get return shipment details by ID */
    @GetMapping("/returns/{id}")
    public ResponseEntity<?> getReturnById(@PathVariable Long id) {
        try {
            ReturnShipmentResponse res = returnService.getReturnById(id);
            return ResponseEntity.ok(ApiResponse.ok(res));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Update return shipment status (e.g. PICKED_UP, IN_TRANSIT, RETURNED, REFUND_INITIATED) */
    @PutMapping("/returns/{id}/status")
    public ResponseEntity<?> updateReturnStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Status is required"));
            }
            ReturnShipment ret = returnService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.ok("Status updated", ReturnShipmentResponse.fromEntity(ret)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** Admin: view all return shipments */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/returns")
    public ResponseEntity<?> getAllReturns() {
        return ResponseEntity.ok(ApiResponse.ok(returnService.getAllReturns()));
    }
}
