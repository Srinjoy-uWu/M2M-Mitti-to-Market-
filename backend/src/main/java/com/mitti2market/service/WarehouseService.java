package com.mitti2market.service;

import com.mitti2market.dto.warehouse.WarehouseRequest;
import com.mitti2market.dto.warehouse.WarehouseResponse;
import com.mitti2market.exception.ResourceNotFoundException;
import com.mitti2market.model.Warehouse;
import com.mitti2market.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepo;

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllActive() {
        return warehouseRepo.findByActiveTrue().stream()
                .map(WarehouseResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAll() {
        return warehouseRepo.findAllByOrderByNameAsc().stream()
                .map(WarehouseResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getById(Long id) {
        Warehouse w = warehouseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));
        return WarehouseResponse.fromEntity(w);
    }

    /**
     * Find nearest active warehouses to given coordinates sorted by haversine distance.
     */
    @Transactional(readOnly = true)
    public List<WarehouseResponse> findNearestTo(double lat, double lng) {
        List<Warehouse> active = warehouseRepo.findByActiveTrue();
        return active.stream()
                .map(w -> {
                    double dist = RouteService.haversineKm(lat, lng, w.getLatitude(), w.getLongitude());
                    WarehouseResponse res = WarehouseResponse.fromEntity(w);
                    res.setDistanceKm(Math.round(dist * 10.0) / 10.0);
                    return res;
                })
                .sorted(Comparator.comparing(WarehouseResponse::getDistanceKm))
                .toList();
    }

    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest req) {
        Warehouse.WarehouseOwnerType ownerType = Warehouse.WarehouseOwnerType.PLATFORM;
        if (req.getOwnerType() != null) {
            try {
                ownerType = Warehouse.WarehouseOwnerType.valueOf(req.getOwnerType().toUpperCase());
            } catch (Exception ignored) {}
        }

        Warehouse w = Warehouse.builder()
                .name(req.getName())
                .ownerType(ownerType)
                .address(req.getAddress())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .capacityKg(req.getCapacityKg() != null ? req.getCapacityKg() : 50000.0)
                .contactPerson(req.getContactPerson())
                .contactPhone(req.getContactPhone())
                .active(req.getActive() != null ? req.getActive() : true)
                .build();

        w = warehouseRepo.save(w);
        log.info("Created warehouse hub {} ({})", w.getName(), w.getId());
        return WarehouseResponse.fromEntity(w);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest req) {
        Warehouse w = warehouseRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", id));

        if (req.getName() != null) w.setName(req.getName());
        if (req.getAddress() != null) w.setAddress(req.getAddress());
        if (req.getLatitude() != null) w.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) w.setLongitude(req.getLongitude());
        if (req.getCapacityKg() != null) w.setCapacityKg(req.getCapacityKg());
        if (req.getContactPerson() != null) w.setContactPerson(req.getContactPerson());
        if (req.getContactPhone() != null) w.setContactPhone(req.getContactPhone());
        if (req.getActive() != null) w.setActive(req.getActive());
        if (req.getOwnerType() != null) {
            try {
                w.setOwnerType(Warehouse.WarehouseOwnerType.valueOf(req.getOwnerType().toUpperCase()));
            } catch (Exception ignored) {}
        }

        w = warehouseRepo.save(w);
        return WarehouseResponse.fromEntity(w);
    }
}
