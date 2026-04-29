package com.hexaweb.backendcluverse.repositories;

import com.hexaweb.backendcluverse.entities.MemberPayment;
import com.hexaweb.backendcluverse.enumerations.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberPaymentRepository extends JpaRepository<MemberPayment, Long> {
    List<MemberPayment> findByClub_Id(Long clubId);
    List<MemberPayment> findByClub_IdAndStatus(Long clubId, PaymentStatus status);
}
