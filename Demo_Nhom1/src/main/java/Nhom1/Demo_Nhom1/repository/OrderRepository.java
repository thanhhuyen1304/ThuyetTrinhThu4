package Nhom1.Demo_Nhom1.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import Nhom1.Demo_Nhom1.model.Order;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable pageable);
    List<Order> findByUserId(String userId);
    Page<Order> findByStatus(String status, Pageable pageable);
    
    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 } }")
    List<Order> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("{ '_id': ?0, 'viewToken': ?1 }")
    Order findByIdAndViewToken(String id, String viewToken);
}
