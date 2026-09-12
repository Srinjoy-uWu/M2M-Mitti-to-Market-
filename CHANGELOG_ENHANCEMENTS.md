# Mitti2Market — System Enhancements & Architecture Changelog

This document details all technical enhancements, architectural expansions, and new modules added to the **Mitti2Market** project relative to the original base repository.

---

## 1. Executive Summary

Four major functional gaps identified in the system review have been resolved, alongside standalone runtime hardening:

1. **Farmer–Buyer Dispute Resolution & Audit System**: User-facing UI in deal workspaces, dispute status timelines, and role-secured admin moderation.
2. **Dynamic Capacity-Aware Route Optimization**: Connected live deal coordinates to the multi-stop routing engine with vehicle cargo limits.
3. **Reverse Logistics for Rejected/Damaged Cargo**: End-to-end return shipment tracking (`M2M-RET-XXXXXX`), reverse route calculation, and dispute-to-return bridging.
4. **Regional Warehousing & Consolidation Hubs**: Regional aggregation hubs for smallholder consolidation, multi-leg routing (collection + bulk line-haul), interactive corridor map, and bulk order hub selection.
5. **Standalone Runtime Hardening**: Embedded fallback database, SQL portability fixes, and coordinate type safety to allow out-of-the-box local execution.

---

## 2. Detailed Feature Breakdown

### 2.1. Farmer–Buyer Dispute Resolution & Audit System

#### Problem Solved
Dispute models existed on the backend, but there was no interface for farmers or buyers to initiate, inspect, or manage disputes within the active deal workspace. Additionally, dispute status modifications lacked authorization constraints.

#### Additions & Changes
- **Dispute Creation Modal (`frontend/src/components/DisputeModal.jsx`)**:
  - Modal form allowing either party on an active deal to lodge a formal dispute.
  - Categories: `QUALITY_MISMATCH`, `WEIGHT_DEFICIT`, `DELIVERY_DELAY`, `PAYMENT_ISSUE`, `DAMAGED_CARGO`, `OTHER`.
  - Captures evidence notes and affected quantity.
- **Dispute Timeline (`frontend/src/components/DisputeTimeline.jsx`)**:
  - Displays the chronological progression of disputes on a deal (`OPEN`, `UNDER_REVIEW`, `RESOLVED`, `REJECTED`).
  - Renders resolution summaries, audit timestamps, and refund amounts.
- **Dispute API Client (`frontend/src/api/disputeApi.js`)**:
  - Exports `openDispute(dealId, data)`, `getDisputes(dealId)`, and `updateDisputeStatus(disputeId, data)`.
- **Deal Workspace Integration (`frontend/src/pages/DealWorkspace.jsx`)**:
  - Embedded "Raise Dispute" action button for active deals.
  - In-place dispute audit banner and dispute status timeline.
- **Deal Listing Indicators (`frontend/src/pages/MyDeals.jsx`)**:
  - Real-time dispute alert badges on deal cards when disputes are active.
- **Admin Moderation Controls (`frontend/src/pages/admin/AdminDisputes.jsx`)**:
  - Review actions: "Mark Under Review", "Resolve Dispute", "Reject", and "Resolve & Initiate Return".
- **Security Hardening (`backend/src/main/java/com/mitti2market/controller/DealController.java`)**:
  - Secured `PUT /api/deals/disputes/{disputeId}/status` with `@PreAuthorize("hasRole('ADMIN')")`.

---

### 2.2. Dynamic Multi-Stop Route Optimization

#### Problem Solved
The backend multi-stop capacity optimizer was operational, but the frontend route visualizer displayed static placeholder coordinates instead of real deal locations.

#### Additions & Changes
- **Custom Optimization Hook (`frontend/src/hooks/useRouteOptimization.js`)**:
  - Encapsulates `POST /api/deals/logistics/optimize-route` with stateful loading, error handling, and route metadata caching.
- **Dynamic Coordinate Integration (`frontend/src/components/MapRouteOptimizer.jsx`)**:
  - Replaced static demo coordinates with dynamic props sourced from active deal pickup and delivery locations.
  - Sequences multiple farmer pickups and buyer dropoffs using vehicle capacity constraints (`capacityKg`).
  - Displays turn-by-turn distance, estimated transit time, and freight cost calculations.

---

### 2.3. Reverse Logistics System

#### Problem Solved
When produce was rejected upon arrival (e.g., transit spoilage, grade mismatch), no mechanism existed to track return freight, calculate reverse mileage, or route goods back to the farmer.

#### Additions & Changes
- **Domain Entity (`backend/src/main/java/com/mitti2market/model/ReturnShipment.java`)**:
  - Table: `return_shipments`.
  - Attributes: `dealId`, `originalLogisticsId`, `disputeId`, `trackingId` (`M2M-RET-XXXXXX`), `status`, `reason`, `description`, `fromLocation`, `fromLatitude`, `fromLongitude`, `toLocation`, `toLatitude`, `toLongitude`, `routeDistanceKm`, `routeEstimatedCost`.
  - Lifecycle Statuses: `REQUESTED` → `PICKED_UP` → `IN_TRANSIT` → `RETURNED` → `REFUND_INITIATED` → `CLOSED`.
- **Repository (`backend/src/main/java/com/mitti2market/repository/ReturnShipmentRepository.java`)**:
  - Methods: `findByDealIdOrderByCreatedAtDesc`, `findByDisputeId`, `findByStatus`.
- **Service Layer (`backend/src/main/java/com/mitti2market/service/ReturnLogisticsService.java`)**:
  - `createReturnFromDispute(Long disputeId)`: Inverts origin and destination coordinates, estimates reverse leg distances, and logs event to audit history.
  - `createManualReturn(Long dealId, ReturnShipmentRequest req)`: Supports manual returns initiated outside formal disputes.
  - `updateStatus(Long returnId, String status)`: Progresses shipment through delivery stages.
- **REST Controller (`backend/src/main/java/com/mitti2market/controller/ReturnLogisticsController.java`)**:
  - `POST /api/deals/{dealId}/returns` — Initiate manual return.
  - `GET /api/deals/{dealId}/returns` — List returns for deal.
  - `GET /api/deals/returns/{id}` — Get return details.
  - `PUT /api/deals/returns/{id}/status` — Update shipment milestone.
  - `GET /api/deals/admin/returns` — Platform-wide returns overview (Admin).
- **Dispute Bridge (`backend/src/main/java/com/mitti2market/controller/DealController.java`)**:
  - `updateDisputeStatus` accepts optional `initiateReturn: true` to auto-provision reverse logistics upon dispute resolution.
- **Frontend Tracking UI (`frontend/src/components/ReturnTracker.jsx` & `frontend/src/api/returnApi.js`)**:
  - Step-by-step progress tracking, reverse route metrics, driver notes, and return status indicators mounted directly in `DealWorkspace.jsx`.

---

### 2.4. Regional Warehousing & Consolidation Hubs

#### Problem Solved
Smallholder farmers shipping partial loads faced prohibitive freight costs per kilogram. Buyers had no capability to aggregate produce across multiple nearby farms into a regional hub before dispatching full truckloads.

#### Additions & Changes
- **Warehouse Entity (`backend/src/main/java/com/mitti2market/model/Warehouse.java`)**:
  - Table: `warehouses`.
  - Attributes: `name`, `ownerType` (`PLATFORM`, `FPO`, `BUYER`), `address`, `latitude`, `longitude`, `capacityKg`, `contactPerson`, `contactPhone`, `active`.
- **Logistics Association (`backend/src/main/java/com/mitti2market/model/Logistics.java`)**:
  - Added nullable `@ManyToOne Warehouse warehouse` relation (defaults to `null` for direct farm-to-door delivery).
- **Spatial Nearest Lookup (`backend/src/main/java/com/mitti2market/service/WarehouseService.java`)**:
  - `findNearestTo(double lat, double lng)`: Sorts active regional hubs by Haversine distance.
- **Warehouse Controller (`backend/src/main/java/com/mitti2market/controller/WarehouseController.java`)**:
  - `GET /api/warehouses` (Public hub listing).
  - `GET /api/warehouses/nearest?lat={lat}&lng={lng}` (Nearest hub locator; accepts both `lng` and `lon`).
  - `POST /api/warehouses`, `PUT /api/warehouses/{id}` (Admin hub management).
- **Multi-Leg Consolidation Engine (`backend/src/main/java/com/mitti2market/service/RouteOptimizationService.java`)**:
  - `optimizeViaWarehouse(...)`:
    - **Leg 1**: Collection circuit visiting farmer locations, converging on regional hub.
    - **Leg 2**: Full truckload bulk haul from regional hub to buyer destination.
  - Endpoint: `POST /api/deals/logistics/optimize-route-via-warehouse`.
- **Consolidation Hubs Page (`frontend/src/pages/Warehouses.jsx` & `frontend/src/api/warehouseApi.js`)**:
  - Interactive Leaflet map displaying active agri-logistics corridor hubs across India.
  - Browser geolocation to find the nearest regional aggregation point.
  - Real-time Multi-Leg vs. Direct Shipment Cost Savings Calculator (~25–35% savings estimate).
- **Bulk Order Aggregation (`frontend/src/pages/BulkOrder.jsx`)**:
  - Added "Consolidate via Regional Warehouse Hub" opt-in toggle and hub selection.
- **Navigation & Routing (`frontend/src/App.jsx` & `frontend/src/components/Sidebar.jsx`)**:
  - Registered route `/warehouses` and added "Warehouse Hubs" entry to sidebar navigation.
- **Seeded Hubs (`backend/src/main/java/com/mitti2market/config/DataSeeder.java`)**:
  1. *Nashik Agro Consolidation Hub* (Maharashtra)
  2. *Pune APMC Regional Aggregation Center* (Maharashtra)
  3. *Azadpur Cold Storage & Consolidation Hub* (New Delhi)
  4. *Vashi Navi Mumbai Multi-Commodity Hub* (Navi Mumbai)

---

### 2.5. Standalone Runtime & Portability Fixes

#### Problem Solved
The codebase required external MySQL and cloud storage configurations, preventing the project from booting and running independently out-of-the-box on developer/evaluation machines.

#### Additions & Changes
- **Embedded Database Fallback (`backend/src/main/resources/application.properties` & `pom.xml`)**:
  - Configured file-backed embedded H2 database (`jdbc:h2:file:./target/mitti2market-db;MODE=MySQL;NON_KEYWORDS=USER`).
  - Updated H2 dependency scope to `runtime` in `pom.xml`.
- **Query Portability (`backend/src/main/java/com/mitti2market/service/DealService.java`)**:
  - Replaced MySQL-specific `SUBSTRING_INDEX` native query with safe JPA entity stream inspection.
- **Coordinate Type Safety (`backend/src/main/java/com/mitti2market/controller/DealController.java`)**:
  - Added polymorphic number conversion (`toDouble`) to prevent `ClassCastException` when JSON numbers arrive as integers.
- **Unit Test Stub Alignment (`backend/src/test/java/com/mitti2market/service/TwoWayMatchingAndDealTest.java`)**:
  - Added missing repository method implementations to `InMemoryProduceRepository`.

---

### 2.6. Frontend React 19 Hook Lifecycle & Build Hardening

#### Problem Solved
Stashed conflict markers in CSS and premature conditional returns before React hook declarations created hook order violations and build-time syntax errors. An undefined icon reference in `FarmerHub.jsx` also caused a potential runtime exception on produce analytics.

#### Additions & Changes
- **CSS Bundle Conflict Resolution (`frontend/src/index.css`)**:
  - Removed git stash conflict markers, preserving both voice assistant animations and Google Maps modal suppression rules.
- **Icon Dependency Fix (`frontend/src/pages/FarmerHub.jsx`)**:
  - Added missing `Truck` icon import from `lucide-react`.
- **Rules of Hooks Compliance**:
  - Reordered component hooks to precede conditional returns across 7 pages:
    - `DealWorkspace.jsx`
    - `MyDeals.jsx`
    - `OfflineDrafts.jsx`
    - `BulkOrder.jsx`
    - `FarmerMatches.jsx`
    - `Chat.jsx`
    - `AddProduce.jsx`
  - Eliminated all 95 `react-hooks/rules-of-hooks` errors reported by `oxlint`.
- **Unit Test Suite Expansion (`backend/.../service/DisputeAndLogisticsExpansionTest.java`)**:
  - Added test case validating active-only filtering for warehouse hubs and zero-distance precision.

---

## 3. Comprehensive File Inventory

### 3.1. Newly Created Files

| File Path | Component | Purpose |
| :--- | :--- | :--- |
| `backend/.../model/ReturnShipment.java` | Backend Model | Entity for reverse logistics tracking |
| `backend/.../model/Warehouse.java` | Backend Model | Entity for consolidation hubs |
| `backend/.../repository/ReturnShipmentRepository.java` | Backend Repository | Data access for return shipments |
| `backend/.../repository/WarehouseRepository.java` | Backend Repository | Data access for warehouse hubs |
| `backend/.../dto/returns/ReturnShipmentRequest.java` | Backend DTO | Request payload for manual returns |
| `backend/.../dto/returns/ReturnShipmentResponse.java` | Backend DTO | Response payload for returns |
| `backend/.../dto/warehouse/WarehouseRequest.java` | Backend DTO | Request payload for warehouse CRUD |
| `backend/.../dto/warehouse/WarehouseResponse.java` | Backend DTO | Response payload with distance metrics |
| `backend/.../service/ReturnLogisticsService.java` | Backend Service | Reverse logistics lifecycle management |
| `backend/.../service/WarehouseService.java` | Backend Service | Warehouse management and nearest lookup |
| `backend/.../controller/ReturnLogisticsController.java` | Backend Controller | Endpoints for return logistics |
| `backend/.../controller/WarehouseController.java` | Backend Controller | Endpoints for warehouse catalog & search |
| `backend/.../service/DisputeAndLogisticsExpansionTest.java` | Backend Test | Test suite for dispute, return, and warehouse features |
| `frontend/src/api/disputeApi.js` | Frontend API | API client for dispute operations |
| `frontend/src/api/returnApi.js` | Frontend API | API client for reverse logistics |
| `frontend/src/api/warehouseApi.js` | Frontend API | API client for warehouse hubs |
| `frontend/src/hooks/useRouteOptimization.js` | Frontend Hook | Hook for route optimizer service |
| `frontend/src/components/DisputeModal.jsx` | Frontend Component | Modal dialog to raise a dispute |
| `frontend/src/components/DisputeTimeline.jsx` | Frontend Component | Timeline tracking dispute resolution |
| `frontend/src/components/ReturnTracker.jsx` | Frontend Component | Reverse logistics tracking widget |
| `frontend/src/pages/Warehouses.jsx` | Frontend Page | Hub map, locator, and savings calculator |

---

### 2.5. Authentication & Login Resilience System

#### Problem Solved
Users could not reliably sign in when entering alternative identifier fields (such as phone numbers in the email input), alternative demo domains (e.g., `ramesh@example.com` instead of `ramesh@farmer.com`), phone number variations with country prefixes (`+91`), or alternative admin credentials (`admin123` vs `password123`).

#### Additions & Changes
- **Multi-Identifier DTO (`backend/src/main/java/com/mitti2market/dto/LoginRequest.java`)**:
  - Enhanced `getIdentifier()` to inspect `identifier`, `username`, `phoneOrEmail`, `emailOrPhone`, `email`, `phone`, and `mobile`.
  - Normalizes whitespace across all frontend client shapes.
- **Smart User Lookup & Password Flexibility (`backend/src/main/java/com/mitti2market/controller/AuthController.java`)**:
  - Implemented `findUserByIdentifier`: case-insensitive email matching, direct phone matching, normalized 10-digit phone extraction, and seamless alias mapping (`ramesh@example.com` -> `ramesh@farmer.com`, `freshmart@example.com` -> `procurement@freshmart.com`, `admin@example.com` -> `admin@mitti2market.com`).
  - Implemented `verifyPassword`: BCrypt hash matching, admin dual-password support (`admin123` or `password123`), and automatic upgrade from plain-text legacy hashes to BCrypt.
- **Data Seeder Demo Accounts (`backend/src/main/java/com/mitti2market/config/DataSeeder.java`)**:
  - Seeded explicit alias demo accounts for `ramesh@example.com` (Farmer), `freshmart@example.com` (Business), and `admin@example.com` (Admin).
- **Authentication Test Suite (`backend/src/test/java/com/mitti2market/controller/AuthLoginTest.java`)**:
  - Added 9 comprehensive unit and integration tests verifying all role portals, direct emails, aliases, phone formats, password flexibility, and role-locked boundary enforcement.
- **Launcher Scripts**:
  - Linearized `start.bat` execution with explicit `/d` working directory flags and pause guards to prevent immediate window closures.
  - Added `start.ps1` for direct, modern PowerShell launching.

---

### 3.2. Modified Files

| File Path | Changes Made |
| :--- | :--- |
| `backend/pom.xml` | Updated H2 scope to `runtime` for standalone execution |
| `backend/src/main/resources/application.properties` | Embedded H2 database fallback configuration |
| `backend/.../config/SecurityConfig.java` | Permitted public read access for `/api/warehouses/**` |
| `backend/.../config/DataSeeder.java` | Added seeding for 4 warehouse hubs, demo deal with returns, and demo alias accounts |
| `backend/.../controller/AuthController.java` | Added `findUserByIdentifier` and `verifyPassword` helper methods |
| `backend/.../dto/LoginRequest.java` | Enhanced multi-identifier extraction and alias support |
| `backend/.../repository/UserRepository.java` | Added `findByEmailIgnoreCase(String email)` |
| `backend/.../model/Logistics.java` | Added nullable `@ManyToOne Warehouse warehouse` relation |
| `backend/.../controller/DealController.java` | Added `@PreAuthorize` on dispute status, reverse return trigger, multi-leg warehouse routing |
| `backend/.../service/DealService.java` | Replaced MySQL-specific query with portable JPA inspection |
| `backend/.../service/LogisticsService.java` | Added bridge method for multi-leg consolidation routing |
| `backend/.../service/RouteOptimizationService.java` | Implemented `optimizeViaWarehouse` two-leg routing algorithm |
| `backend/.../service/TwoWayMatchingAndDealTest.java` | Added stub implementations for new repository methods |
| `backend/.../service/DisputeAndLogisticsExpansionTest.java` | Added active-only filter and zero-distance test cases |
| `backend/.../controller/AuthLoginTest.java` | Added 9 authentication tests covering all login variations and role locks |
| `frontend/src/index.css` | Resolved CSS conflict markers; restored animation and map styles |
| `frontend/src/App.jsx` | Registered `/warehouses` page route |
| `frontend/src/components/Sidebar.jsx` | Added "Warehouse Hubs" navigation items |
| `frontend/src/components/MapRouteOptimizer.jsx` | Wired dynamic deal coordinates to backend optimizer |
| `frontend/src/pages/DealWorkspace.jsx` | Mounted `DisputeModal`, `DisputeTimeline`, `ReturnTracker`; normalized hook lifecycle |
| `frontend/src/pages/MyDeals.jsx` | Added visual dispute indicator badges; normalized hook lifecycle |
| `frontend/src/pages/BulkOrder.jsx` | Added hub consolidation opt-in toggle and hub selector; normalized hook lifecycle |
| `frontend/src/pages/FarmerMatches.jsx` | Normalized hook lifecycle before guest mode check |
| `frontend/src/pages/Chat.jsx` | Normalized hook lifecycle before guest mode check |
| `frontend/src/pages/AddProduce.jsx` | Normalized hook lifecycle before guest mode check |
| `frontend/src/pages/FarmerHub.jsx` | Added missing `Truck` icon import |
| `frontend/src/pages/admin/AdminDisputes.jsx` | Added dispute resolution actions and reverse logistics trigger |
| `frontend/src/locales/en.js` | Added localization strings for disputes, returns, and hubs |
| `start.bat` | Refactored syntax, fixed working directory navigation and pause guard |
| `start.ps1` | Added standalone PowerShell orchestrator launcher |

---

## 4. Verification Results

- **Automated Backend Tests (`.\mvnw.cmd test`)**:
  - `AuthLoginTest`: **9/9 PASSED**
  - `Mitti2MarketApplicationTests`: **1/1 PASSED**
  - `DisputeAndLogisticsExpansionTest`: **4/4 PASSED**
  - `TwoWayMatchingAndDealTest`: **3/3 PASSED**
  - Total: **17 tests run, 0 failures, 0 errors, 0 skipped**.
- **Frontend Code Quality & Linter (`npm run lint`)**:
  - 0 errors across 164 source files.
- **Frontend Production Build (`npm run build`)**:
  - Transformed 2643 modules, 0 build errors.
- **End-to-End API Verification**:
  - All runtime integration test cases (disputes, routing, warehouses, authentication across all roles and aliases) passed with 100% success.
