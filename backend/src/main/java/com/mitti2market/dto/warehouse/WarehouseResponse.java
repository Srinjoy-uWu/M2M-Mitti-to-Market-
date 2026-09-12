package com.mitti2market.dto.warehouse;

import com.mitti2market.model.Warehouse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseResponse {
    private Long id;
    private String name;
    private String ownerType;
    private String address;
    private Double latitude;
    private Double longitude;
    private Double capacityKg;
    private String contactPerson;
    private String contactPhone;
    private Boolean active;
    private Double distanceKm; // Computed when querying nearest
    private LocalDateTime createdAt;

    public static WarehouseResponse fromEntity(Warehouse w) {
        if (w == null) return null;
        return WarehouseResponse.builder()
                .id(w.getId())
                .name(w.getName())
                .ownerType(w.getOwnerType() != null ? w.getOwnerType().name() : null)
                .address(w.getAddress())
                .latitude(w.getLatitude())
                .longitude(w.getLongitude())
                .capacityKg(w.getCapacityKg())
                .contactPerson(w.getContactPerson())
                .contactPhone(w.getContactPhone())
                .active(w.getActive())
                .createdAt(w.getCreatedAt())
                .build();
    }
}
