package com.mitti2market.dto.returns;

import com.mitti2market.model.ReturnShipment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnShipmentResponse {
    private Long id;
    private Long dealId;
    private Long originalLogisticsId;
    private Long disputeId;
    private String trackingId;
    private String status;
    private String reason;
    private String description;
    private String fromLocation;
    private Double fromLatitude;
    private Double fromLongitude;
    private String toLocation;
    private Double toLatitude;
    private Double toLongitude;
    private Double routeDistanceKm;
    private Double routeEstimatedCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime returnedAt;

    public static ReturnShipmentResponse fromEntity(ReturnShipment s) {
        if (s == null) return null;
        return ReturnShipmentResponse.builder()
                .id(s.getId())
                .dealId(s.getDealId())
                .originalLogisticsId(s.getOriginalLogisticsId())
                .disputeId(s.getDisputeId())
                .trackingId(s.getTrackingId())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .reason(s.getReason())
                .description(s.getDescription())
                .fromLocation(s.getFromLocation())
                .fromLatitude(s.getFromLatitude())
                .fromLongitude(s.getFromLongitude())
                .toLocation(s.getToLocation())
                .toLatitude(s.getToLatitude())
                .toLongitude(s.getToLongitude())
                .routeDistanceKm(s.getRouteDistanceKm())
                .routeEstimatedCost(s.getRouteEstimatedCost())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .pickedUpAt(s.getPickedUpAt())
                .returnedAt(s.getReturnedAt())
                .build();
    }
}
