package Nhom1.Demo_Nhom1.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import Nhom1.Demo_Nhom1.model.PendingPayment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PendingPaymentRepository extends MongoRepository<PendingPayment, String> {
    Optional<PendingPayment> findByIdAndStatus(String id, String status);
    List<PendingPayment> findByUserId(String userId);
    List<PendingPayment> findByExpiresAtBeforeAndStatus(LocalDateTime dateTime, String status);
}
