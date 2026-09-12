
package com.mitti2market.config;

import com.mitti2market.model.BuyerInterest;
import com.mitti2market.model.Deal;
import com.mitti2market.model.Dispute;
import com.mitti2market.model.Logistics;
import com.mitti2market.model.Produce;
import com.mitti2market.model.Produce.ProduceStatus;
import com.mitti2market.model.ReturnShipment;
import com.mitti2market.model.User;
import com.mitti2market.model.User.Role;
import com.mitti2market.model.Warehouse;
import com.mitti2market.repository.BuyerInterestRepository;
import com.mitti2market.repository.DealRepository;
import com.mitti2market.repository.DisputeRepository;
import com.mitti2market.repository.LogisticsRepository;
import com.mitti2market.repository.ProduceRepository;
import com.mitti2market.repository.ReturnShipmentRepository;
import com.mitti2market.repository.UserRepository;
import com.mitti2market.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProduceRepository produceRepository;
    private final BuyerInterestRepository interestRepository;
    private final WarehouseRepository warehouseRepository;
    private final DealRepository dealRepository;
    private final LogisticsRepository logisticsRepository;
    private final DisputeRepository disputeRepository;
    private final ReturnShipmentRepository returnShipmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        log.info("==============================================");
        log.info("Starting Mitti2Market development data seeder");
        log.info("==============================================");

        String hashedPassword = passwordEncoder.encode("password123");

        /*
         * IMPORTANT:
         *
         * This seeder NEVER deletes users.
         *
         * Earlier implementation deleted every user except a few demo accounts.
         * That caused MySQL foreign-key errors because tables such as notifications,
         * orders, messages, appeals, documents, etc. can reference users.
         *
         * The new implementation is idempotent:
         *
         *     User exists     -> reuse existing user
         *     User missing    -> create user
         *
         * The same approach is used for demo produce and buyer interests.
         */

        // ============================================================
        // 1. ADMIN
        // ============================================================

        User admin = getOrCreateUser(
                "admin@mitti2market.com",
                () -> User.builder()
                        .name("System Admin")
                        .email("admin@mitti2market.com")
                        .passwordHash(hashedPassword)
                        .phone("9999999999")
                        .role(Role.ADMIN)
                        .location("HQ - New Delhi")
                        .organizationName("Mitti2Market Admin Operations")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .build()
        );

        getOrCreateUser(
                "admin@example.com",
                () -> User.builder()
                        .name("System Admin (Demo)")
                        .email("admin@example.com")
                        .passwordHash(hashedPassword)
                        .phone("9999999998")
                        .role(Role.ADMIN)
                        .location("HQ - New Delhi")
                        .organizationName("Mitti2Market Admin Operations")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .build()
        );

        // ============================================================
        // 2. FARMERS (4 Realistic Indian Names: 2 Male, 2 Female)
        // ============================================================

        User farmer1 = getOrCreateUser(
                "ramesh@farmer.com",
                () -> User.builder()
                        .name("Ramesh Kumar")
                        .email("ramesh@farmer.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543210")
                        .role(Role.FARMER)
                        .location("Pune, Maharashtra")
                        .organizationName("Kisan FPO Pune")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .build()
        );

        getOrCreateUser(
                "ramesh@example.com",
                () -> User.builder()
                        .name("Ramesh Kumar (Demo)")
                        .email("ramesh@example.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543201")
                        .role(Role.FARMER)
                        .location("Pune, Maharashtra")
                        .organizationName("Kisan FPO Pune")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .build()
        );

        // Female Farmer 1: Sunita Devi
        User farmer2 = getOrCreateUser(
                "sunita@farmer.com",
                () -> User.builder()
                        .name("Sunita Devi")
                        .email("sunita@farmer.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543211")
                        .role(Role.FARMER)
                        .location("Nashik, Maharashtra")
                        .organizationName("Devi Farm Collective")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .build()
        );

        // Male Farmer 2: Balwinder Singh
        User farmer3 = getOrCreateUser(
                "balwinder@farmer.com",
                () -> User.builder()
                        .name("Balwinder Singh")
                        .email("balwinder@farmer.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543212")
                        .role(Role.FARMER)
                        .location("Ludhiana, Punjab")
                        .organizationName("Punjab Agro Farmers Producer Co.")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .build()
        );

        // Female Farmer 2: Anusuiya Patel
        User farmer4 = getOrCreateUser(
                "anusuiya@farmer.com",
                () -> User.builder()
                        .name("Anusuiya Patel")
                        .email("anusuiya@farmer.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543213")
                        .role(Role.FARMER)
                        .location("Anand, Gujarat")
                        .organizationName("Amrut Krishi FPO")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .build()
        );

        // ============================================================
        // 3. BUSINESSES (4 Realistic Business Accounts)
        // ============================================================

        // Business 1: FreshMart Agro Procurement Pvt Ltd
        User business1 = getOrCreateUser(
                "procurement@freshmart.com",
                () -> User.builder()
                        .name("FreshMart Agro Procurement")
                        .email("procurement@freshmart.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543220")
                        .role(Role.BUSINESS)
                        .location("Mumbai, Maharashtra")
                        .organizationName("FreshMart Agro Procurement Pvt Ltd")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .rating(4.5)
                        .build()
        );

        getOrCreateUser(
                "freshmart@example.com",
                () -> User.builder()
                        .name("FreshMart Demo")
                        .email("freshmart@example.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543202")
                        .role(Role.BUSINESS)
                        .location("Mumbai, Maharashtra")
                        .organizationName("FreshMart Agro Procurement Pvt Ltd")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .rating(4.5)
                        .build()
        );

        // Business 2: Reliance Fresh Retail Ltd
        User business2 = getOrCreateUser(
                "purchase@reliancefresh.com",
                () -> User.builder()
                        .name("Reliance Fresh Sourcing")
                        .email("purchase@reliancefresh.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543221")
                        .role(Role.BUSINESS)
                        .location("Navi Mumbai, Maharashtra")
                        .organizationName("Reliance Retail Agri Supply Ltd")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .rating(4.8)
                        .build()
        );

        // Business 3: BigBasket Wholesale India
        User business3 = getOrCreateUser(
                "sourcing@bigbasket.com",
                () -> User.builder()
                        .name("BigBasket Farmer Connect")
                        .email("sourcing@bigbasket.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543222")
                        .role(Role.BUSINESS)
                        .location("Bengaluru, Karnataka")
                        .organizationName("Supermarket Grocery Supplies Pvt Ltd (BigBasket)")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .rating(4.7)
                        .build()
        );

        // Business 4: ITC Choupal Fresh Ltd
        User business4 = getOrCreateUser(
                "procure@itcchoupal.com",
                () -> User.builder()
                        .name("ITC e-Choupal Procurement")
                        .email("procure@itcchoupal.com")
                        .passwordHash(hashedPassword)
                        .phone("9876543223")
                        .role(Role.BUSINESS)
                        .location("Indore, Madhya Pradesh")
                        .organizationName("ITC Limited - Agri Business Division")
                        .verified(true)
                        .verificationStatus(User.VerificationStatus.VERIFIED)
                        .rating(4.9)
                        .build()
        );

        // ============================================================
        // 4. DEMO PRODUCE
        // ============================================================

        // Farmer 1 (Ramesh Kumar - Pune) Produce
        createProduceIfMissing(
                farmer1,
                "Alphonso Mango",
                "Fruits",
                500,
                "kg",
                120.0,
                "Fresh Ratnagiri Alphonso mangoes, Grade A",
                "Pune, Maharashtra",
                "https://example.com/alphonso.jpg",
                ProduceStatus.AVAILABLE,
                108.0,
                132.0
        );

        createProduceIfMissing(
                farmer1,
                "Red Onion",
                "Vegetables",
                2000,
                "kg",
                25.0,
                "Nashik red onions, freshly harvested",
                "Pune, Maharashtra",
                "https://example.com/onion.jpg",
                ProduceStatus.AVAILABLE,
                22.5,
                27.5
        );

        createProduceIfMissing(
                farmer1,
                "Turmeric Powder",
                "Spices",
                100,
                "kg",
                180.0,
                "Organic turmeric, high curcumin content",
                "Pune, Maharashtra",
                "https://example.com/turmeric.jpg",
                ProduceStatus.AVAILABLE,
                162.0,
                198.0
        );

        // Farmer 2 (Sunita Devi - Nashik) Produce
        createProduceIfMissing(
                farmer2,
                "Thompson Seedless Grapes",
                "Fruits",
                800,
                "kg",
                60.0,
                "Nashik valley grapes, export quality",
                "Nashik, Maharashtra",
                "https://example.com/grapes.jpg",
                ProduceStatus.LOW_STOCK,
                54.0,
                66.0
        );

        createProduceIfMissing(
                farmer2,
                "Green Chilli",
                "Vegetables",
                300,
                "kg",
                40.0,
                "Fresh green chillies, medium hot",
                "Nashik, Maharashtra",
                "https://example.com/chilli.jpg",
                ProduceStatus.AVAILABLE,
                36.0,
                44.0
        );

        createProduceIfMissing(
                farmer2,
                "Jowar (Sorghum)",
                "Grains",
                1500,
                "kg",
                32.0,
                "Premium jowar grain, pesticide-free",
                "Nashik, Maharashtra",
                "https://example.com/jowar.jpg",
                ProduceStatus.AVAILABLE,
                28.8,
                35.2
        );

        // Farmer 3 (Balwinder Singh - Ludhiana, Punjab) Produce
        createProduceIfMissing(
                farmer3,
                "Sharbati Wheat",
                "Grains",
                3000,
                "kg",
                28.0,
                "Golden grain premium Sharbati wheat, harvest freshly bagged",
                "Ludhiana, Punjab",
                "https://example.com/wheat.jpg",
                ProduceStatus.AVAILABLE,
                26.0,
                31.0
        );

        createProduceIfMissing(
                farmer3,
                "Basmati Rice 1121",
                "Grains",
                2500,
                "kg",
                85.0,
                "Aromatic extra long grain Basmati 1121 steam paddy",
                "Ludhiana, Punjab",
                "https://example.com/basmati.jpg",
                ProduceStatus.AVAILABLE,
                80.0,
                92.0
        );

        // Farmer 4 (Anusuiya Patel - Anand, Gujarat) Produce
        createProduceIfMissing(
                farmer4,
                "Castor Seeds",
                "Oilseeds",
                1200,
                "kg",
                62.0,
                "High oil-content Grade-A castor seeds",
                "Anand, Gujarat",
                "https://example.com/castor.jpg",
                ProduceStatus.AVAILABLE,
                58.0,
                66.0
        );

        createProduceIfMissing(
                farmer4,
                "Cumin Seeds (Jeera)",
                "Spices",
                600,
                "kg",
                240.0,
                "Cleaned, machine-sorted Gujarat export cumin seed",
                "Anand, Gujarat",
                "https://example.com/cumin.jpg",
                ProduceStatus.AVAILABLE,
                225.0,
                260.0
        );

        // ============================================================
        // 5. TOMATO DEMO LISTING FOR AI DEAL ADVISOR
        // ============================================================

        Produce tomato = getOrCreateProduce(
                farmer1,
                "Tomato",
                () -> Produce.builder()
                        .farmer(farmer1)
                        .name("Tomato")
                        .category("Vegetables")
                        .quantity(2000)
                        .unit("kg")
                        .pricePerUnit(25.0)
                        .description("Fresh farm tomatoes, Grade A, harvest-ready")
                        .location("Pune, Maharashtra")
                        .imageUrl("https://example.com/tomato.jpg")
                        .status(ProduceStatus.AVAILABLE)
                        .aiSuggestedMinPrice(22.5)
                        .aiSuggestedMaxPrice(27.5)
                        .build()
        );

        // ============================================================
        // 6. BUYER OFFERS FOR TOMATO
        // ============================================================

        createInterestIfMissing(
                business1,
                farmer1,
                tomato,
                29.0,
                2000,
                "FreshMart can take full quantity at ₹29/kg with Mumbai warehouse delivery."
        );

        createInterestIfMissing(
                business2,
                farmer1,
                tomato,
                27.0,
                1500,
                "Reliance Fresh Pune hub pickup, ₹27/kg for 1500 kg instant payment."
        );

        createInterestIfMissing(
                business3,
                farmer1,
                tomato,
                28.0,
                1000,
                "BigBasket sourcing 1000 kg Grade A sorting at ₹28/kg."
        );

        createInterestIfMissing(
                business4,
                farmer1,
                tomato,
                28.5,
                1200,
                "ITC Choupal direct procurement at ₹28.5/kg for 1200 kg."
        );

        seedSampleWarehouses();
        seedSampleDeals(farmer1, business1);

        log.info("==============================================");
        log.info("Mitti2Market development data seeding complete");
        log.info("Admin     : admin@mitti2market.com");
        log.info("Farmers   : Ramesh Kumar, Sunita Devi, Balwinder Singh, Anusuiya Patel");
        log.info("Businesses: FreshMart, Reliance Fresh, BigBasket, ITC e-Choupal");
        log.info("All passwords: password123");
        log.info("==============================================");
    }

    // ================================================================
    // USER HELPER
    // ================================================================

    private User getOrCreateUser(
            String email,
            java.util.function.Supplier<User> userSupplier) {

        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = userSupplier.get();
                    User savedUser = userRepository.save(user);

                    log.info("Created demo user: {}", email);

                    return savedUser;
                });
    }

    // ================================================================
    // PRODUCE HELPER
    // ================================================================

    private Produce getOrCreateProduce(
            User farmer,
            String produceName,
            java.util.function.Supplier<Produce> produceSupplier) {

        return produceRepository.findAll()
                .stream()
                .filter(produce -> produce.getFarmer() != null)
                .filter(produce -> produce.getFarmer().getId() != null)
                .filter(produce -> farmer.getId() != null)
                .filter(produce -> produce.getFarmer().getId().equals(farmer.getId()))
                .filter(produce -> produce.getName() != null)
                .filter(produce -> produce.getName().equalsIgnoreCase(produceName))
                .findFirst()
                .orElseGet(() -> {
                    Produce produce = produceSupplier.get();
                    Produce savedProduce = produceRepository.save(produce);

                    log.info(
                            "Created demo produce: {} for {}",
                            produceName,
                            farmer.getEmail()
                    );

                    return savedProduce;
                });
    }

    // ================================================================
    // NORMAL PRODUCE HELPER
    // ================================================================

    private void createProduceIfMissing(
            User farmer,
            String name,
            String category,
            Integer quantity,
            String unit,
            double pricePerUnit,
            String description,
            String location,
            String imageUrl,
            ProduceStatus status,
            double aiMinPrice,
            double aiMaxPrice) {

        getOrCreateProduce(
                farmer,
                name,
                () -> Produce.builder()
                        .farmer(farmer)
                        .name(name)
                        .category(category)
                        .quantity(quantity)
                        .unit(unit)
                        .pricePerUnit(pricePerUnit)
                        .description(description)
                        .location(location)
                        .imageUrl(imageUrl)
                        .status(status)
                        .aiSuggestedMinPrice(aiMinPrice)
                        .aiSuggestedMaxPrice(aiMaxPrice)
                        .build()
        );
    }

    // ================================================================
    // BUYER INTEREST HELPER
    // ================================================================

    private void createInterestIfMissing(
            User buyer,
            User farmer,
            Produce produce,
            double offeredPrice,
            Integer offeredQuantity,
            String message) {

        boolean exists = interestRepository.findAll()
                .stream()
                .anyMatch(interest ->
                        interest.getBuyer() != null
                                && interest.getBuyer().getId() != null
                                && buyer.getId() != null
                                && interest.getBuyer().getId().equals(buyer.getId())

                                && interest.getFarmer() != null
                                && interest.getFarmer().getId() != null
                                && farmer.getId() != null
                                && interest.getFarmer().getId().equals(farmer.getId())

                                && interest.getProduce() != null
                                && interest.getProduce().getId() != null
                                && produce.getId() != null
                                && interest.getProduce().getId().equals(produce.getId())

                                && Double.compare(
                                        interest.getOfferedPrice(),
                                        offeredPrice
                                ) == 0
                );

        if (!exists) {

            interestRepository.save(
                    BuyerInterest.builder()
                            .buyer(buyer)
                            .farmer(farmer)
                            .produce(produce)
                            .offeredPrice(offeredPrice)
                            .offeredQuantity(offeredQuantity)
                            .message(message)
                            .status(BuyerInterest.InterestStatus.PENDING)
                            .build()
            );

            log.info(
                    "Created demo buyer offer: {} -> {} at ₹{}/kg",
                    buyer.getEmail(),
                    produce.getName(),
                    offeredPrice
            );
        }
    }

    private void seedSampleWarehouses() {
        if (warehouseRepository.count() > 0) {
            return;
        }

        List<Warehouse> hubs = List.of(
                Warehouse.builder()
                        .name("Nashik Agro Consolidation Hub")
                        .ownerType(Warehouse.WarehouseOwnerType.PLATFORM)
                        .address("Plot 42, MIDC Ambad, Nashik, Maharashtra 422010")
                        .latitude(19.9975)
                        .longitude(73.7898)
                        .capacityKg(100000.0)
                        .contactPerson("Sunil Deshmukh")
                        .contactPhone("9823011223")
                        .active(true)
                        .build(),
                Warehouse.builder()
                        .name("Pune APMC Regional Aggregation Center")
                        .ownerType(Warehouse.WarehouseOwnerType.PLATFORM)
                        .address("Market Yard, Gultekdi, Pune, Maharashtra 411037")
                        .latitude(18.4965)
                        .longitude(73.8670)
                        .capacityKg(150000.0)
                        .contactPerson("Rajesh Kadam")
                        .contactPhone("9822055667")
                        .active(true)
                        .build(),
                Warehouse.builder()
                        .name("Azadpur Cold Storage & Consolidation Hub")
                        .ownerType(Warehouse.WarehouseOwnerType.PLATFORM)
                        .address("Gate 4, New Subzi Mandi, Azadpur, Delhi 110033")
                        .latitude(28.7164)
                        .longitude(77.1738)
                        .capacityKg(250000.0)
                        .contactPerson("Virender Sharma")
                        .contactPhone("9811099887")
                        .active(true)
                        .build(),
                Warehouse.builder()
                        .name("Vashi Navi Mumbai Multi-Commodity Hub")
                        .ownerType(Warehouse.WarehouseOwnerType.PLATFORM)
                        .address("Sector 19, APMC Grain Market, Vashi, Navi Mumbai 400705")
                        .latitude(19.0760)
                        .longitude(73.0039)
                        .capacityKg(200000.0)
                        .contactPerson("Mahesh Patel")
                        .contactPhone("9820033445")
                        .active(true)
                        .build()
        );

        warehouseRepository.saveAll(hubs);
        log.info("Seeded {} regional aggregation warehouse hubs", hubs.size());
    }

    private void seedSampleDeals(User farmer, User buyer) {
        if (dealRepository.count() == 0 && farmer != null && buyer != null) {
            List<Warehouse> hubs = warehouseRepository.findAll();

            Deal deal1 = Deal.builder()
                    .dealId("M2M-2026-10001")
                    .farmer(farmer)
                    .buyer(buyer)
                    .cropName("Fresh Red Tomatoes")
                    .quantity(2500)
                    .unit("kg")
                    .agreedPrice(24.0)
                    .totalAmount(60000.0)
                    .pickupLocation("Ramesh Kumar Farm, Dindori, Nashik")
                    .pickupLatitude(20.08)
                    .pickupLongitude(73.82)
                    .deliveryLocation("FreshMart Central Depot, Andheri West, Mumbai")
                    .deliveryLatitude(19.11)
                    .deliveryLongitude(72.85)
                    .status(Deal.DealStatus.IN_TRANSIT)
                    .conversationId("conv_ramesh_freshmart_1")
                    .build();

            dealRepository.save(deal1);

            Logistics log1 = Logistics.builder()
                    .deal(deal1)
                    .trackingId("M2M-TRK-77001")
                    .type(Logistics.LogisticsType.MITTI2MARKET)
                    .status(Logistics.LogisticsStatus.IN_TRANSIT)
                    .pickupLocation("Ramesh Kumar Farm, Dindori, Nashik")
                    .pickupLatitude(20.08)
                    .pickupLongitude(73.82)
                    .deliveryLocation("FreshMart Central Depot, Andheri West, Mumbai")
                    .deliveryLatitude(19.11)
                    .deliveryLongitude(72.85)
                    .routeDistanceKm(165.4)
                    .routeEstimatedCost(2840.0)
                    .vehicleNumber("MH-15-EG-4421")
                    .transporterName("M2M Cold-Express Freight")
                    .warehouse(hubs.isEmpty() ? null : hubs.get(0))
                    .build();

            logisticsRepository.save(log1);

            Dispute disp1 = Dispute.builder()
                    .dealId(deal1.getId())
                    .raisedBy(buyer)
                    .reason(Dispute.DisputeReason.DAMAGED_GOODS)
                    .description("Transit inspection noted 350 kg crushed tomatoes due to crate shifting.")
                    .status(Dispute.DisputeStatus.UNDER_REVIEW)
                    .build();

            disputeRepository.save(disp1);

            ReturnShipment ret1 = ReturnShipment.builder()
                    .dealId(deal1.getId())
                    .originalLogisticsId(log1.getId())
                    .disputeId(disp1.getId())
                    .trackingId("M2M-RET-88401A")
                    .status(ReturnShipment.ReturnStatus.IN_TRANSIT)
                    .reason("Transit cargo damage (350 kg)")
                    .description("Reverse freight routing from FreshMart Mumbai back to Ramesh Kumar Farm in Nashik.")
                    .fromLocation("FreshMart Central Depot, Andheri West, Mumbai")
                    .fromLatitude(19.11)
                    .fromLongitude(72.85)
                    .toLocation("Ramesh Kumar Farm, Dindori, Nashik")
                    .toLatitude(20.08)
                    .toLongitude(73.82)
                    .routeDistanceKm(165.4)
                    .routeEstimatedCost(2250.0)
                    .build();

            returnShipmentRepository.save(ret1);
            log.info("Seeded sample deal M2M-2026-10001 with logistics, dispute, and return shipment");
        }
    }
}

