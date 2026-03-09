package Nhom1.Demo_Nhom1.repository;

import Nhom1.Demo_Nhom1.model.AdminActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminActivityRepository extends MongoRepository<AdminActivity, String> {
    List<AdminActivity> findByAdminUsernameOrderByTimestampDesc(String adminUsername);
    List<AdminActivity> findByActionOrderByTimestampDesc(String action);
    List<AdminActivity> findByEntityTypeOrderByTimestampDesc(String entityType);
    List<AdminActivity> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
    List<AdminActivity> findTop50ByOrderByTimestampDesc();
    List<AdminActivity> findTop100ByOrderByTimestampDesc();
}
