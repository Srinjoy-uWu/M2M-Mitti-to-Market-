package com.mitti2market.dto.returns;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnShipmentRequest {
    private String reason;
    private String description;
    private String fromLocation;
    private Double fromLatitude;
    private Double fromLongitude;
    private String toLocation;
    private Double toLatitude;
    private Double toLongitude;
    private Long disputeId;
}
