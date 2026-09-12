package com.mitti2market.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseRequest {
    private String name;
    private String ownerType; // PLATFORM, FPO, BUYER
    private String address;
    private Double latitude;
    private Double longitude;
    private Double capacityKg;
    private String contactPerson;
    private String contactPhone;
    private Boolean active;
}
