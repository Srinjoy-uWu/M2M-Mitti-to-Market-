package com.mitti2market.service;

import com.mitti2market.dto.warehouse.WarehouseResponse;
import com.mitti2market.model.ReturnShipment;
import com.mitti2market.model.Warehouse;
import com.mitti2market.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class DisputeAndLogisticsExpansionTest {

    private InMemoryWarehouseRepository warehouseRepo;
    private WarehouseService warehouseService;
    private RouteOptimizationService routeOptimizationService;

    @BeforeEach
    void setUp() {
        warehouseRepo = new InMemoryWarehouseRepository();
        warehouseService = new WarehouseService(warehouseRepo);

        HaversineRouteService routeService = new HaversineRouteService(40.0, "");
        LogisticsCostService costService = new LogisticsCostService(100.0, 12.0, 1200.0, 1.5, 0.25, 0.005);
        routeOptimizationService = new RouteOptimizationService(routeService, costService);
    }

    @Test
    void testWarehouseNearestCalculation() {
        Warehouse w1 = Warehouse.builder()
                .id(1L)
                .name("Nashik Consolidation Hub")
                .address("Plot 42, MIDC Ambad, Nashik, Maharashtra")
                .latitude(20.00)
                .longitude(73.78)
                .capacityKg(100000.0)
                .active(true)
                .build();

        Warehouse w2 = Warehouse.builder()
                .id(2L)
                .name("Pune APMC Aggregation Center")
                .address("Market Yard, Gultekdi, Pune, Maharashtra")
                .latitude(18.52)
                .longitude(73.85)
                .capacityKg(80000.0)
                .active(true)
                .build();

        Warehouse w3 = Warehouse.builder()
                .id(3L)
                .name("Azadpur Mandi Logistics Hub")
                .address("Block B, Azadpur Mandi, New Delhi")
                .latitude(28.71)
                .longitude(77.17)
                .capacityKg(150000.0)
                .active(true)
                .build();

        warehouseRepo.save(w1);
        warehouseRepo.save(w2);
        warehouseRepo.save(w3);

        // Coordinates near Shirdi (Maharashtra): 19.89 N, 74.47 E
        List<WarehouseResponse> nearest = warehouseService.findNearestTo(19.89, 74.47);

        assertNotNull(nearest);
        assertEquals(3, nearest.size());
        // Nashik should be the closest (~70 km)
        assertEquals("Nashik Consolidation Hub", nearest.get(0).getName());
        assertTrue(nearest.get(0).getDistanceKm() < nearest.get(1).getDistanceKm());
        // Pune is second, Delhi is farthest
        assertEquals("Pune APMC Aggregation Center", nearest.get(1).getName());
        assertEquals("Azadpur Mandi Logistics Hub", nearest.get(2).getName());
    }

    @Test
    void testMultiLegRouteOptimizationViaWarehouse() {
        List<Map<String, Object>> farmerPickups = new ArrayList<>();

        Map<String, Object> farmA = new HashMap<>();
        farmA.put("name", "Farm Alpha (Nashik North)");
        farmA.put("lat", 20.08);
        farmA.put("lng", 73.82);
        farmA.put("weightKg", 1200.0);
        farmerPickups.add(farmA);

        Map<String, Object> farmB = new HashMap<>();
        farmB.put("name", "Farm Beta (Niphad East)");
        farmB.put("lat", 20.06);
        farmB.put("lng", 74.11);
        farmB.put("weightKg", 1800.0);
        farmerPickups.add(farmB);

        double[] warehouseCoords = new double[]{20.00, 73.78}; // Nashik Hub
        double[] buyerCoords = new double[]{19.07, 72.87};     // Mumbai APMC

        Map<String, Object> result = routeOptimizationService.optimizeViaWarehouse(
                farmerPickups, warehouseCoords, buyerCoords, 5000.0);

        assertNotNull(result);
        assertTrue(result.containsKey("leg1DistanceKm"));
        assertTrue(result.containsKey("leg2DistanceKm"));
        assertTrue(result.containsKey("totalDistanceKm"));
        assertTrue(result.containsKey("orderedStops"));

        double leg1 = ((Number) result.get("leg1DistanceKm")).doubleValue();
        double leg2 = ((Number) result.get("leg2DistanceKm")).doubleValue();
        double total = ((Number) result.get("totalDistanceKm")).doubleValue();

        assertTrue(leg1 > 0, "Leg 1 distance should be greater than 0");
        assertTrue(leg2 > 0, "Leg 2 distance should be greater than 0");
        assertEquals(Math.round((leg1 + leg2) * 10.0) / 10.0, total, 0.5);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orderedStops = (List<Map<String, Object>>) result.get("orderedStops");
        assertEquals(2, orderedStops.size());
    }

    @Test
    void testReturnShipmentTrackingIdFormatAndLifecycle() {
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String trackingId = "M2M-RET-" + randomSuffix;

        assertTrue(trackingId.matches("^M2M-RET-[0-9A-Z]{6}$"), "Tracking ID should match M2M-RET-XXXXXX format");

        ReturnShipment shipment = ReturnShipment.builder()
                .id(101L)
                .dealId(10L)
                .trackingId(trackingId)
                .status(ReturnShipment.ReturnStatus.REQUESTED)
                .reason("Damaged produce during transit")
                .fromLocation("FreshMart Buyer Warehouse, Mumbai")
                .fromLatitude(19.07)
                .fromLongitude(72.87)
                .toLocation("Ramesh Kumar Farm, Nashik")
                .toLatitude(20.00)
                .toLongitude(73.78)
                .routeDistanceKm(165.0)
                .routeEstimatedCost(2850.0)
                .build();

        assertEquals(ReturnShipment.ReturnStatus.REQUESTED, shipment.getStatus());

        // Progress status to PICKED_UP
        shipment.setStatus(ReturnShipment.ReturnStatus.PICKED_UP);
        assertEquals(ReturnShipment.ReturnStatus.PICKED_UP, shipment.getStatus());

        // Progress status to IN_TRANSIT
        shipment.setStatus(ReturnShipment.ReturnStatus.IN_TRANSIT);
        assertEquals(ReturnShipment.ReturnStatus.IN_TRANSIT, shipment.getStatus());

        // Progress status to RETURNED
        shipment.setStatus(ReturnShipment.ReturnStatus.RETURNED);
        assertEquals(ReturnShipment.ReturnStatus.RETURNED, shipment.getStatus());
    }

    @Test
    void testWarehouseActiveOnlyFilter() {
        Warehouse activeWh = Warehouse.builder()
                .id(10L)
                .name("Active Mandi Hub")
                .address("Hub Road, Delhi")
                .latitude(28.70)
                .longitude(77.10)
                .capacityKg(50000.0)
                .active(true)
                .build();

        Warehouse inactiveWh = Warehouse.builder()
                .id(11L)
                .name("Decommissioned Hub")
                .address("Old Closed Yard, Delhi")
                .latitude(28.71)
                .longitude(77.11)
                .capacityKg(20000.0)
                .active(false)
                .build();

        warehouseRepo.save(activeWh);
        warehouseRepo.save(inactiveWh);

        List<WarehouseResponse> results = warehouseService.findNearestTo(28.70, 77.10);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Active Mandi Hub", results.get(0).getName());
        assertEquals(0.0, results.get(0).getDistanceKm(), 0.1);
    }

    /**
     * In-memory WarehouseRepository for fast isolated testing without DB setup.
     */
    static class InMemoryWarehouseRepository implements WarehouseRepository {
        private final Map<Long, Warehouse> store = new HashMap<>();

        @Override
        public List<Warehouse> findByActiveTrue() {
            return store.values().stream()
                    .filter(w -> Boolean.TRUE.equals(w.getActive()))
                    .toList();
        }

        @Override
        public List<Warehouse> findAllByOrderByNameAsc() {
            return store.values().stream().sorted(Comparator.comparing(Warehouse::getName)).toList();
        }

        @Override
        public <S extends Warehouse> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId((long) (store.size() + 1));
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Warehouse> findById(Long aLong) {
            return Optional.ofNullable(store.get(aLong));
        }

        @Override
        public boolean existsById(Long aLong) {
            return store.containsKey(aLong);
        }

        @Override
        public List<Warehouse> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<Warehouse> findAllById(Iterable<Long> longs) {
            List<Warehouse> result = new ArrayList<>();
            for (Long id : longs) {
                if (store.containsKey(id)) result.add(store.get(id));
            }
            return result;
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void deleteById(Long aLong) {
            store.remove(aLong);
        }

        @Override
        public void delete(Warehouse entity) {
            if (entity.getId() != null) store.remove(entity.getId());
        }

        @Override
        public void deleteAllById(Iterable<? extends Long> longs) {
            for (Long id : longs) store.remove(id);
        }

        @Override
        public void deleteAll(Iterable<? extends Warehouse> entities) {
            for (Warehouse entity : entities) {
                if (entity.getId() != null) store.remove(entity.getId());
            }
        }

        @Override
        public void deleteAll() {
            store.clear();
        }

        @Override
        public <S extends Warehouse> List<S> saveAll(Iterable<S> entities) {
            List<S> res = new ArrayList<>();
            for (S e : entities) res.add(save(e));
            return res;
        }

        @Override public void flush() {}
        @Override public <S extends Warehouse> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Warehouse> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
        @Override public void deleteAllInBatch(Iterable<Warehouse> entities) { deleteAll(entities); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) { deleteAllById(longs); }
        @Override public void deleteAllInBatch() { deleteAll(); }
        @Override public Warehouse getOne(Long aLong) { return store.get(aLong); }
        @Override public Warehouse getById(Long aLong) { return store.get(aLong); }
        @Override public Warehouse getReferenceById(Long aLong) { return store.get(aLong); }
        @Override public <S extends Warehouse> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
        @Override public <S extends Warehouse> List<S> findAll(Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Warehouse> List<S> findAll(Example<S> example, Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Warehouse> Page<S> findAll(Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Warehouse> long count(Example<S> example) { return 0; }
        @Override public <S extends Warehouse> boolean exists(Example<S> example) { return false; }
        @Override public <S extends Warehouse, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<Warehouse> findAll(Sort sort) { return findAll(); }
        @Override public Page<Warehouse> findAll(Pageable pageable) { return null; }
    }
}
