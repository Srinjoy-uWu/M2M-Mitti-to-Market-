package com.mitti2market.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "return_shipments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long dealId;

    private Long originalLogisticsId;

    private Long disputeId;

    @Column(unique = true, nullable = false, length = 40)
    private String trackingId; // e.g. M2M-RET-XXXXXX

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(length = 100)
    private String reason;

    @Column(length = 1000)
    private String description;

    // Origin of return (typically the buyer destination)
    private String fromLocation;
    private Double fromLatitude;
    private Double fromLongitude;

    // Return destination (typically the original farmer farm / warehouse)
    private String toLocation;
    private Double toLatitude;
    private Double toLongitude;

    // Route calculation metrics
    private Double routeDistanceKm;
    private Double routeEstimatedCost;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime pickedUpAt;
    private LocalDateTime returnedAt;

    public enum ReturnStatus {
        REQUESTED,
        PICKED_UP,
        IN_TRANSIT,
        RETURNED,
        REFUND_INITIATED,
        CLOSED
    }
}
