package com.mitti2market.service;

import com.mitti2market.dto.RouteEstimate;
import com.mitti2market.dto.returns.ReturnShipmentRequest;
import com.mitti2market.dto.returns.ReturnShipmentResponse;
import com.mitti2market.exception.BadRequestException;
import com.mitti2market.exception.ResourceNotFoundException;
import com.mitti2market.model.Deal;
import com.mitti2market.model.Dispute;
import com.mitti2market.model.Logistics;
import com.mitti2market.model.ReturnShipment;
import com.mitti2market.repository.DealRepository;
import com.mitti2market.repository.DisputeRepository;
import com.mitti2market.repository.LogisticsRepository;
import com.mitti2market.repository.ReturnShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnLogisticsService {

    private final ReturnShipmentRepository returnRepo;
    private final DealRepository dealRepo;
    private final LogisticsRepository logisticsRepo;
    private final DisputeRepository disputeRepo;
    private final RouteService routeService;
    private final LogisticsCostService costService;
    private final DealStateMachineService stateMachine;

    private static final AtomicLong RET_COUNTER = new AtomicLong(200000);

    /**
     * Creates a reverse logistics return shipment from a resolved dispute.
     * Reverses delivery coordinates so rejected produce is routed back to the farmer.
     */
    @Transactional
    public ReturnShipment createReturnFromDispute(Long disputeId) {
        Dispute dispute = disputeRepo.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute", "id", disputeId));

        Long dealId = dispute.getDealId();
        Deal deal = dealRepo.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", "id", dealId));

        Logistics originalLogistics = logisticsRepo.findByDealId(dealId).orElse(null);

        String fromLoc = originalLogistics != null && originalLogistics.getDeliveryLocation() != null
                ? originalLogistics.getDeliveryLocation()
                : deal.getDeliveryLocation();
        Double fromLat = originalLogistics != null && originalLogistics.getDeliveryLatitude() != null
                ? originalLogistics.getDeliveryLatitude()
                : deal.getDeliveryLatitude();
        Double fromLng = originalLogistics != null && originalLogistics.getDeliveryLongitude() != null
                ? originalLogistics.getDeliveryLongitude()
                : deal.getDeliveryLongitude();

        String toLoc = originalLogistics != null && originalLogistics.getPickupLocation() != null
                ? originalLogistics.getPickupLocation()
                : deal.getPickupLocation();
        Double toLat = originalLogistics != null && originalLogistics.getPickupLatitude() != null
                ? originalLogistics.getPickupLatitude()
                : deal.getPickupLatitude();
        Double toLng = originalLogistics != null && originalLogistics.getPickupLongitude() != null
                ? originalLogistics.getPickupLongitude()
                : deal.getPickupLongitude();

        Double distanceKm = null;
        Double estimatedCost = null;

        if (fromLat != null && fromLng != null && toLat != null && toLng != null) {
            try {
                RouteEstimate est = routeService.estimateRoute(fromLat, fromLng, toLat, toLng);
                distanceKm = est.getDistanceKm();
                var costMap = costService.estimateCost(est, deal.getQuantity() != null ? deal.getQuantity() : 1000.0);
                estimatedCost = costMap.get("total") instanceof Number ? ((Number) costMap.get("total")).doubleValue() : null;
            } catch (Exception e) {
                log.warn("Could not calculate return route estimate for deal {}: {}", dealId, e.getMessage());
            }
        }

        String trackingId = "M2M-RET-" + RET_COUNTER.incrementAndGet();

        ReturnShipment ret = ReturnShipment.builder()
                .dealId(dealId)
                .originalLogisticsId(originalLogistics != null ? originalLogistics.getId() : null)
                .disputeId(disputeId)
                .trackingId(trackingId)
                .status(ReturnShipment.ReturnStatus.REQUESTED)
                .reason(dispute.getReason() != null ? dispute.getReason().name() : "DISPUTE_RETURN")
                .description("Dispute return initiated: " + (dispute.getDescription() != null ? dispute.getDescription() : ""))
                .fromLocation(fromLoc)
                .fromLatitude(fromLat)
                .fromLongitude(fromLng)
                .toLocation(toLoc)
                .toLatitude(toLat)
                .toLongitude(toLng)
                .routeDistanceKm(distanceKm)
                .routeEstimatedCost(estimatedCost)
                .build();

        ret = returnRepo.save(ret);

        stateMachine.recordEvent(dealId, "RETURN_INITIATED",
                dispute.getRaisedBy() != null ? dispute.getRaisedBy().getId() : null,
                "SYSTEM",
                "Reverse logistics initiated: " + trackingId + " to return produce to farmer",
                null);

        log.info("Initiated return shipment {} for dispute {} on deal {}", trackingId, disputeId, dealId);
        return ret;
    }

    /**
     * Manual return creation without an existing formal dispute.
     */
    @Transactional
    public ReturnShipment createManualReturn(Long dealId, ReturnShipmentRequest req) {
        Deal deal = dealRepo.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", "id", dealId));

        Logistics originalLogistics = logisticsRepo.findByDealId(dealId).orElse(null);

        String fromLoc = req.getFromLocation() != null ? req.getFromLocation() :
                (originalLogistics != null ? originalLogistics.getDeliveryLocation() : deal.getDeliveryLocation());
        Double fromLat = req.getFromLatitude() != null ? req.getFromLatitude() :
                (originalLogistics != null ? originalLogistics.getDeliveryLatitude() : deal.getDeliveryLatitude());
        Double fromLng = req.getFromLongitude() != null ? req.getFromLongitude() :
                (originalLogistics != null ? originalLogistics.getDeliveryLongitude() : deal.getDeliveryLongitude());

        String toLoc = req.getToLocation() != null ? req.getToLocation() :
                (originalLogistics != null ? originalLogistics.getPickupLocation() : deal.getPickupLocation());
        Double toLat = req.getToLatitude() != null ? req.getToLatitude() :
                (originalLogistics != null ? originalLogistics.getPickupLatitude() : deal.getPickupLatitude());
        Double toLng = req.getToLongitude() != null ? req.getToLongitude() :
                (originalLogistics != null ? originalLogistics.getPickupLongitude() : deal.getPickupLongitude());

        Double distanceKm = null;
        Double estimatedCost = null;

        if (fromLat != null && fromLng != null && toLat != null && toLng != null) {
            try {
                RouteEstimate est = routeService.estimateRoute(fromLat, fromLng, toLat, toLng);
                distanceKm = est.getDistanceKm();
                var costMap = costService.estimateCost(est, deal.getQuantity() != null ? deal.getQuantity() : 1000.0);
                estimatedCost = costMap.get("total") instanceof Number ? ((Number) costMap.get("total")).doubleValue() : null;
            } catch (Exception e) {
                log.warn("Could not calculate return route estimate for deal {}: {}", dealId, e.getMessage());
            }
        }

        String trackingId = "M2M-RET-" + RET_COUNTER.incrementAndGet();

        ReturnShipment ret = ReturnShipment.builder()
                .dealId(dealId)
                .originalLogisticsId(originalLogistics != null ? originalLogistics.getId() : null)
                .disputeId(req.getDisputeId())
                .trackingId(trackingId)
                .status(ReturnShipment.ReturnStatus.REQUESTED)
                .reason(req.getReason() != null ? req.getReason() : "MANUAL_RETURN")
                .description(req.getDescription())
                .fromLocation(fromLoc)
                .fromLatitude(fromLat)
                .fromLongitude(fromLng)
                .toLocation(toLoc)
                .toLatitude(toLat)
                .toLongitude(toLng)
                .routeDistanceKm(distanceKm)
                .routeEstimatedCost(estimatedCost)
                .build();

        return returnRepo.save(ret);
    }

    @Transactional
    public ReturnShipment updateStatus(Long returnId, String statusStr) {
        ReturnShipment ret = returnRepo.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnShipment", "id", returnId));

        ReturnShipment.ReturnStatus nextStatus;
        try {
            nextStatus = ReturnShipment.ReturnStatus.valueOf(statusStr.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid return status: " + statusStr);
        }

        ret.setStatus(nextStatus);
        LocalDateTime now = LocalDateTime.now();
        if (nextStatus == ReturnShipment.ReturnStatus.PICKED_UP && ret.getPickedUpAt() == null) {
            ret.setPickedUpAt(now);
        } else if (nextStatus == ReturnShipment.ReturnStatus.RETURNED && ret.getReturnedAt() == null) {
            ret.setReturnedAt(now);
        }

        return returnRepo.save(ret);
    }

    @Transactional(readOnly = true)
    public List<ReturnShipmentResponse> getReturnsForDeal(Long dealId) {
        return returnRepo.findByDealIdOrderByCreatedAtDesc(dealId).stream()
                .map(ReturnShipmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReturnShipmentResponse getReturnById(Long returnId) {
        ReturnShipment ret = returnRepo.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnShipment", "id", returnId));
        return ReturnShipmentResponse.fromEntity(ret);
    }

    @Transactional(readOnly = true)
    public List<ReturnShipmentResponse> getAllReturns() {
        return returnRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(ReturnShipmentResponse::fromEntity)
                .toList();
    }
}
