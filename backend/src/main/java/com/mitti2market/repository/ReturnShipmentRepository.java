package com.mitti2market.repository;

import com.mitti2market.model.ReturnShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnShipmentRepository extends JpaRepository<ReturnShipment, Long> {

    List<ReturnShipment> findByDealIdOrderByCreatedAtDesc(Long dealId);

    Optional<ReturnShipment> findByDisputeId(Long disputeId);

    Optional<ReturnShipment> findByTrackingId(String trackingId);

    List<ReturnShipment> findAllByOrderByCreatedAtDesc();
}
