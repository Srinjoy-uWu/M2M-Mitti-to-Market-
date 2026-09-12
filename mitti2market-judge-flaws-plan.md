# Mitti2Market — Plan to Fix the 4 Judge-Flagged Gaps

This plan is based on actually reading the codebase (not the README). For each item I list **what already exists**, **why the judge probably flagged it anyway**, and **exactly what to add or touch** — split into new files (safe) vs. modified files (needs care). A "Do Not Touch" list is included per section and one master list at the end, so nothing that already works gets disturbed.

**Golden rule for every change below:** prefer *new files* and *new optional fields/endpoints* over editing existing methods. Where an existing file must change, the change is additive (new field with a safe default, new method, new optional request key) — never a rename, deletion, or signature change to something already in use.

---

## Priority order (do them in this sequence)

| # | Item | Effort | Backend risk | Why this order |
|---|------|--------|--------------|-----------------|
| 1 | Farmer–buyer dispute UI | Low | Very low | Backend is 95% done — fastest visible win |
| 2 | Route optimization | Low–Medium | Very low | Backend already works — this is a wiring job |
| 3 | Reverse logistics | Medium | Low | Builds directly on dispute infra from #1 |
| 4 | Warehouse | Medium–High | Low | Biggest new surface — do last so #1–#3 patterns are proven first |

---

## 1. Farmer–Buyer Dispute Resolution

### Current state (verified in code)
The backend is **already fully built and working**:
- `model/Dispute.java` — full entity with `DisputeReason` enum (QUALITY_ISSUE, QUANTITY_ISSUE, LATE_DELIVERY, DAMAGED_GOODS, MISSING_GOODS, PAYMENT_ISSUE, OTHER) and `DisputeStatus` (OPEN, UNDER_REVIEW, RESOLVED, REJECTED).
- `DealController.java` already has:
  - `POST /api/deals/{dealId}/disputes` — opens a dispute, with a real ownership check (only the farmer or buyer on that deal can raise one).
  - `GET /api/deals/{dealId}/disputes` — lists disputes for a deal.
  - `PUT /api/deals/disputes/{disputeId}/status` — updates status.
  - Every dispute event is already logged to the deal timeline via `stateMachine.recordEvent(...)`.
- `AdminDisputes.jsx` already exists — but only for the **admin** side.

**The actual gap:** there is no farmer/buyer-facing UI anywhere in the frontend to raise or view a dispute. That's almost certainly what the judge meant — the feature exists but is invisible to the two people it's for.

**One real backend gap found while reviewing this:** `PUT /api/deals/disputes/{disputeId}/status` has no role check — any authenticated user (not just an admin) can currently resolve/reject any dispute, because `SecurityConfig` only restricts `/api/admin/**`, not this endpoint. Worth closing as part of this work.

### What to build

**New frontend files (safe — pure additions):**
- `frontend/src/components/DisputeModal.jsx` — form to raise a dispute (reason dropdown + description), calls existing `POST /api/deals/{dealId}/disputes`.
- `frontend/src/components/DisputeTimeline.jsx` — read-only list of disputes for a deal, calls existing `GET /api/deals/{dealId}/disputes`. Show status as a badge (OPEN / UNDER_REVIEW / RESOLVED / REJECTED).
- `frontend/src/api/disputeApi.js` — thin wrapper: `openDispute(dealId, data)`, `getDisputes(dealId)`. (Mirror the pattern already used in `frontend/src/api/dealApi.js` — don't touch that file, just add a sibling.)

**Modified frontend files (additive only):**
- `frontend/src/pages/DealWorkspace.jsx` — add a "Raise a Dispute" button that opens `DisputeModal`, and render `DisputeTimeline`. This is adding a new button + two new component mounts to an existing page — do not remove or reorder existing sections.
- `frontend/src/pages/MyDeals.jsx` — add a small dispute-status indicator per deal row if `getDisputes` returns anything open. Purely additive badge, no change to existing row logic.
- Add translation keys (`raiseDispute`, `disputeReason.*`, `disputeStatus.*`) to `frontend/src/locales/*.js` and `frontend/src/data/translations/index.js` — additive keys only, don't touch existing keys.

**Modified backend file (one small, additive fix):**
- `backend/src/main/java/com/mitti2market/controller/DealController.java` — add `@PreAuthorize("hasRole('ADMIN')")` to `updateDisputeStatus` only. This doesn't change the method body or signature, just restricts who can call it — safe because no legitimate current caller is a non-admin.

### Do not touch
- `Dispute.java`, `DisputeRepository.java`, `openDispute`, `getDisputes` — these already work correctly, including the ownership check. Leave the method bodies exactly as they are.
- `AdminDisputes.jsx` — leave the admin flow untouched; you're adding a parallel farmer/buyer flow, not replacing this.

### Verification checklist
- [ ] Farmer can open a dispute on a deal they're part of; buyer on the same deal can see it.
- [ ] A user not part of the deal still gets rejected (existing check — just confirm it still fires).
- [ ] A non-admin logged-in user can no longer call the status-update endpoint directly (test with Postman/curl).
- [ ] Admin dashboard (`AdminDisputes.jsx`) still resolves disputes exactly as before.

---

## 2. Route Optimization

### Current state (verified in code)
This is the most surprising finding: **the backend route optimizer is already built, non-trivial, and correct.**
- `service/RouteOptimizationService.java` implements a capacity-aware nearest-neighbor heuristic over a haversine distance matrix — multi-stop, respects vehicle capacity in kg, skips stops that don't fit.
- `service/HaversineRouteService.java` will use a real OSRM road-routing API if `ROUTING_API_URL` is configured, and only falls back to straight-line distance × 1.3 road factor if it isn't.
- `POST /api/deals/logistics/optimize-route` exposes this to the frontend already.
- `frontend/src/api/dealApi.js` already has an `optimizeRoute()` wrapper function for it.

**The actual gap:** `frontend/src/components/MapRouteOptimizer.jsx` (792 lines, rendered on `FarmerDashboard.jsx`) has a comment at the top literally reading *"DEMO FARMER & DESTINATION DATA ... replace with real backend data when available"* — it renders hardcoded fake markers and **never calls `optimizeRoute()` at all.** The judge saw a route map, but it isn't real. This is a wiring problem, not a missing-algorithm problem.

### What to build

**Modified frontend file (the core fix — additive, no removals):**
- `frontend/src/components/MapRouteOptimizer.jsx`:
  - Replace the hardcoded `DEMO FARMER & DESTINATION DATA` block with a prop/fetch that pulls the real farmer pickup points and buyer delivery point for the current deal/order (from `dealApi.js` — likely `getDeal(dealId)` or similar, already exists).
  - Call the existing `optimizeRoute(body)` from `dealApi.js` instead of computing anything locally.
  - Keep every existing UI element (map, markers, popups, the info panel) — only the data source changes, not the rendering logic.

**New frontend file (optional but recommended):**
- `frontend/src/hooks/useRouteOptimization.js` — small hook wrapping the `optimizeRoute` call + loading/error state, so `MapRouteOptimizer.jsx` stays clean. Purely additive.

**No backend changes required for the core fix.** If you also want real road distances (not just haversine), that's a config-only change:
- Set `ROUTING_API_URL` in `backend/.env` to a public/self-hosted OSRM endpoint. `HaversineRouteService` already supports this — zero code changes needed, it already checks for this at startup.

### Do not touch
- `RouteOptimizationService.java`, `HaversineRouteService.java`, `RouteService.java` interface — this logic is correct and already tested implicitly through the endpoint. Do not "improve" the algorithm as part of this fix; that's a separate, optional enhancement (see below).
- `POST /api/deals/logistics/optimize-route` — don't change its request/response shape, since that's the contract `MapRouteOptimizer.jsx` needs to match.

### Optional (only if time remains — not required to close the judge's flaw)
- Swap the nearest-neighbor heuristic for a proper 2-opt improvement pass. This would be a **new method** on `RouteOptimizationService` called after the existing heuristic, not a rewrite of it — keeps the current working path as a guaranteed fallback.

### Verification checklist
- [ ] Opening the route optimizer on a real deal shows the actual farmer/buyer coordinates, not "Demo Farmer 1/2/3".
- [ ] The displayed distance/time/cost matches what `POST /api/deals/logistics/optimize-route` returns for those coordinates (spot-check with curl).
- [ ] Existing single-route views (`GET /api/deals/logistics/{logisticsId}/route`) still render correctly — you didn't touch this endpoint, so this should be a no-op check.

---

## 3. Reverse Logistics

### Current state (verified in code)
**Nothing exists for this today.** `Logistics.LogisticsStatus` only has a one-directional flow: `REQUESTED → ASSIGNED → PICKUP_SCHEDULED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED`. There's no return leg, no "rejected at delivery" path, nothing that sends goods back to the farmer.

This is a real, fair gap — but it connects naturally to the dispute system you're wiring up in section 1, since disputes already carry reasons like `DAMAGED_GOODS`, `QUALITY_ISSUE`, `MISSING_GOODS` that would legitimately trigger a return.

### What to build

**New backend files (safe additions, new domain, doesn't touch existing tables):**
- `model/ReturnShipment.java` — new entity: `id`, `dealId`, `originalLogisticsId`, `disputeId` (nullable — a return can exist without a formal dispute, e.g. buyer simply refuses at the door), `reason`, `fromLocation/toLocation` (mirrors the original delivery, reversed), `trackingId` (pattern `M2M-RET-XXXXXX`, following the existing `M2M-TRK-XXXXXX` convention), `status` enum: `REQUESTED, PICKED_UP, IN_TRANSIT, RETURNED, REFUND_INITIATED, CLOSED`, timestamps.
- `repository/ReturnShipmentRepository.java`
- `service/ReturnLogisticsService.java`:
  - `createReturnFromDispute(Long disputeId)` — looks up the dispute + its deal's `Logistics` record, builds a `ReturnShipment` with pickup/delivery swapped from the original, reuses `routeService.estimateRoute(...)` for the reverse-leg estimate (same interface, no changes needed there).
  - `createManualReturn(Long dealId, reason, description)` — for returns without a dispute.
  - `updateStatus(Long returnId, status)`.
- `controller/ReturnLogisticsController.java`:
  - `POST /api/deals/{dealId}/returns`
  - `GET /api/deals/{dealId}/returns`
  - `PUT /api/deals/returns/{id}/status`
  (New controller, new route prefix — cannot collide with anything existing.)
- `dto/returns/ReturnShipmentRequest.java`, `ReturnShipmentResponse.java`

**Modified backend file (one additive hook, optional key with a safe default):**
- `backend/src/main/java/com/mitti2market/controller/DealController.java`, inside `updateDisputeStatus`: read an optional `initiateReturn` boolean from the existing request body map (defaults to `false` if absent, so **every existing caller behaves exactly as before**). When `true` and status is `RESOLVED`, call `returnLogisticsService.createReturnFromDispute(disputeId)`. This is a few added lines inside the existing `try` block, not a rewrite.

**New frontend files:**
- `frontend/src/components/ReturnTracker.jsx` — mirrors the look of the existing `LogisticsTracking.jsx` / `OrderTracker.jsx` components but for a `ReturnShipment`. (Look at `LogisticsTracking.jsx` for the visual pattern to copy — don't import from it, just match the style.)
- `frontend/src/api/returnApi.js` — wrapper for the three new endpoints.

**Modified frontend file (additive):**
- `frontend/src/pages/admin/AdminDisputes.jsx` — add an "Resolve & Initiate Return" action alongside the existing Resolve/Reject buttons. Existing buttons and their handlers stay exactly as-is.

### Do not touch
- `model/Logistics.java` and its `LogisticsStatus` enum — **do not add return-related values to this enum.** Existing code almost certainly has switch/if statements over `LogisticsStatus` elsewhere (frontend status badges, admin views) that assume it only ever reaches `DELIVERED`; adding values there risks silently breaking those. A separate `ReturnShipment` entity avoids this entirely.
- `DealStateMachineService.java` — don't route return logic through the deal state machine; keep it as its own simple status flow on `ReturnShipment`.

### Verification checklist
- [ ] Resolving a dispute *without* `initiateReturn` behaves exactly as it does today (regression check — this is the important one).
- [ ] Resolving a dispute *with* `initiateReturn: true` creates a `ReturnShipment` row with sensible reversed coordinates and a route estimate.
- [ ] A return can also be created manually (no dispute), for the "buyer just refuses delivery" case.

---

## 4. Warehouse / Consolidation Hub

### Current state (verified in code)
**Nothing exists.** "Warehouse" only appears as flavor text — buyer delivery addresses like `"FreshMart Warehouse, Andheri"` in mock data and a placeholder string in `BulkOrder.jsx`. There is no `Warehouse` entity, no concept of a shared collection point, no multi-leg routing (farmer → hub → buyer). Every delivery today is modeled as one direct leg.

This is the biggest of the four asks — it's a genuinely new module, not a wiring fix — so scope it carefully for a hackathon timeline. A minimal but real version: **a small number of fixed collection points that small farmers can route produce to, consolidated into one larger shipment to the buyer**, which is the standard "aggregation" pitch judges expect for this kind of SIH problem statement.

### What to build

**New backend files (fully additive — new table, no changes to existing ones):**
- `model/Warehouse.java` — `id`, `name`, `ownerType` (`PLATFORM`, `FPO`, `BUYER`), `address`, `latitude`, `longitude`, `capacityKg`, `contactPerson`, `contactPhone`, `active`.
- `repository/WarehouseRepository.java` — include a `findNearest(lat, lng)`-style query (can start simple: fetch all active warehouses and sort by haversine distance in the service layer, matching the style already used in `RouteOptimizationService`).
- `service/WarehouseService.java` — CRUD + `findNearestTo(double lat, double lng)`.
- `controller/WarehouseController.java`:
  - `GET /api/warehouses` (public — buyers/farmers need to see available hubs)
  - `POST /api/warehouses`, `PUT /api/warehouses/{id}` (admin-only, `@PreAuthorize("hasRole('ADMIN')")`, matching the existing pattern in `AdminController.java`)
- `dto/warehouse/WarehouseRequest.java`, `WarehouseResponse.java`
- Seed 3–5 sample warehouses in `DataSeeder.java` **as a new method appended to the class**, called alongside the existing seed calls — don't modify the existing seed methods, just add one more call in the same style.

**Modified backend file (additive field only — this is the one that needs the most care):**
- `model/Logistics.java` — add:
  ```java
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id")
  private Warehouse warehouse; // nullable — null means "direct delivery", exactly like today
  ```
  This is a nullable foreign key. Every existing row has it as `null` and every existing direct-delivery flow is completely unaffected — `hibernate.ddl-auto=update` will add the column without touching existing data. **Do not make this field non-nullable, and do not add a new `LogisticsType` enum value for it** — keep the existing `OWN`/`MITTI2MARKET` distinction exactly as it is; "via warehouse or not" is an orthogonal, optional attribute.

**Modified backend file (new method only, existing methods untouched):**
- `service/RouteOptimizationService.java` — add a new method, e.g. `optimizeViaWarehouse(List<double[]> farmerPickups, double[] warehouseCoords, double[] buyerCoords, Double capacityKg)`, which internally calls the **existing** `optimize(...)` method twice (farmers → warehouse leg, then warehouse → buyer as a single stop) and combines the two `RouteEstimate` results. This is pure addition — the existing `optimize()` signature and behavior are untouched, so every current caller keeps working exactly as before.
- `controller/DealController.java` — add one new endpoint, `POST /api/deals/logistics/optimize-route-via-warehouse`, calling the new service method. Don't modify the existing `/optimize-route` endpoint.

**New frontend files:**
- `frontend/src/pages/Warehouses.jsx` (or an admin sub-page) — list warehouses, admin CRUD.
- `frontend/src/api/warehouseApi.js` — wrapper for the new endpoints.

**Modified frontend file (additive UI only):**
- `frontend/src/pages/BulkOrder.jsx` — add an optional "Consolidate via warehouse" toggle. When on, call the nearest-warehouse lookup and the new `optimize-route-via-warehouse` endpoint instead of the direct one. When off (default), **everything behaves exactly as it does today** — this must be an opt-in addition, not a replacement of the existing flow.

### Do not touch
- Existing `Logistics` rows / existing direct-delivery logistics flow — the nullable `warehouse` field guarantees this, but double-check no existing query does `SELECT * FROM logistics WHERE warehouse_id = ...` assuming it's always set.
- `RouteOptimizationService.optimize(...)` — the existing single-leg/multi-stop method must keep its current signature and behavior; the warehouse feature only adds a new method that calls it.
- `BulkOrder.jsx`'s existing direct-order submission path — must remain the default and must not require a warehouse selection.

### Verification checklist
- [ ] A normal (non-warehouse) bulk order still works exactly as before — this is the most important regression check for this section.
- [ ] Creating a warehouse via admin, then choosing "consolidate via warehouse" on a new order, produces two route legs with sensible combined distance/cost.
- [ ] Existing `Logistics` records (created before this change) still display correctly everywhere they're shown, with `warehouse = null`.

---

## Cross-cutting notes

- **Dispute → Reverse logistics** is the one real dependency between these four items: build #1 (dispute UI) before #3 (reverse logistics), since #3's main trigger is a resolved dispute.
- **Route optimization → Warehouse** are related but independent: #2 fixes the *existing* direct-delivery route feature; #4 adds a *new* multi-leg mode on top of it later. Don't build #4's multi-leg logic until #2's single-leg wiring is confirmed working — you'll reuse the same `optimize()` call path.
- None of the four items require touching `SecurityConfig.java`'s existing rules except the one `@PreAuthorize` addition in section 1 — new endpoints should get their own explicit rules added there (as new lines), never by loosening an existing rule.

## Master "Do Not Touch" list

- `RouteOptimizationService.optimize(...)`, `HaversineRouteService`, `RouteService` interface — working, tested by use, keep as-is.
- `Dispute.java`, `DisputeRepository.java`, existing `openDispute`/`getDisputes` bodies — working, keep as-is.
- `Logistics.LogisticsStatus` enum — do not add values to it.
- `DealStateMachineService.java` — don't route new features through it.
- Any existing `.env`/`application.properties` keys — all new config should be new keys with sensible defaults, never renamed or repurposed existing ones.
- `BulkOrder.jsx`'s current direct-order path and `MapRouteOptimizer.jsx`'s existing map rendering — only their data sources change, not their structure.

## Suggested build order

1. Dispute UI (frontend only, ~1–2 days) + the one `@PreAuthorize` fix.
2. Route optimization wiring (frontend only, ~1–2 days, or +1 day if you also stand up an OSRM instance for real road distances).
3. Reverse logistics (new backend module + small frontend, ~2–3 days).
4. Warehouse (new backend module + new frontend page + `Logistics` field + new route-optimization method, ~3–4 days).
