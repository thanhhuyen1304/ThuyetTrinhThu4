package Nhom1.Demo_Nhom1.service;

import Nhom1.Demo_Nhom1.model.AdminActivity;
import Nhom1.Demo_Nhom1.repository.AdminActivityRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminActivityService {
    
    @Autowired
    private AdminActivityRepository activityRepository;
    
    public void logActivity(String action, String entityType, String entityId, String description) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                
                AdminActivity activity = new AdminActivity();
                activity.setAdminUsername(username);
                activity.setAction(action);
                activity.setEntityType(entityType);
                activity.setEntityId(entityId);
                activity.setDescription(description);
                activity.setTimestamp(LocalDateTime.now());
                
                activityRepository.save(activity);
            }
        } catch (Exception e) {
            System.err.println("Failed to log admin activity: " + e.getMessage());
        }
    }
    
    public void logActivity(String action, String entityType, String entityId, String entityName, String description, HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                
                AdminActivity activity = new AdminActivity();
                activity.setAdminUsername(username);
                activity.setAction(action);
                activity.setEntityType(entityType);
                activity.setEntityId(entityId);
                activity.setEntityName(entityName);
                activity.setDescription(description);
                activity.setTimestamp(LocalDateTime.now());
                
                if (request != null) {
                    activity.setIpAddress(getClientIp(request));
                    activity.setUserAgent(request.getHeader("User-Agent"));
                }
                
                activityRepository.save(activity);
            }
        } catch (Exception e) {
            System.err.println("Failed to log admin activity: " + e.getMessage());
        }
    }
    
    public void logActivityWithChanges(String action, String entityType, String entityId, String entityName, 
                                      String description, String oldValue, String newValue, HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                
                AdminActivity activity = new AdminActivity();
                activity.setAdminUsername(username);
                activity.setAction(action);
                activity.setEntityType(entityType);
                activity.setEntityId(entityId);
                activity.setEntityName(entityName);
                activity.setDescription(description);
                activity.setOldValue(oldValue);
                activity.setNewValue(newValue);
                activity.setTimestamp(LocalDateTime.now());
                
                if (request != null) {
                    activity.setIpAddress(getClientIp(request));
                    activity.setUserAgent(request.getHeader("User-Agent"));
                }
                
                activityRepository.save(activity);
            }
        } catch (Exception e) {
            System.err.println("Failed to log admin activity: " + e.getMessage());
        }
    }
    
    public void logActivityWithMetadata(String action, String entityType, String entityId, String entityName, 
                                       String description, String oldValue, String newValue, 
                                       java.util.Map<String, Object> metadata, HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();
                
                AdminActivity activity = new AdminActivity();
                activity.setAdminUsername(username);
                activity.setAction(action);
                activity.setEntityType(entityType);
                activity.setEntityId(entityId);
                activity.setEntityName(entityName);
                activity.setDescription(description);
                activity.setOldValue(oldValue);
                activity.setNewValue(newValue);
                activity.setMetadata(metadata);
                activity.setTimestamp(LocalDateTime.now());
                
                if (request != null) {
                    activity.setIpAddress(getClientIp(request));
                    activity.setUserAgent(request.getHeader("User-Agent"));
                }
                
                activityRepository.save(activity);
            }
        } catch (Exception e) {
            System.err.println("Failed to log admin activity: " + e.getMessage());
        }
    }
    
    public List<AdminActivity> getRecentActivities(int limit) {
        if (limit <= 50) {
            return activityRepository.findTop50ByOrderByTimestampDesc();
        }
        return activityRepository.findTop100ByOrderByTimestampDesc();
    }
    
    public List<AdminActivity> getActivitiesByAdmin(String username) {
        return activityRepository.findByAdminUsernameOrderByTimestampDesc(username);
    }
    
    public List<AdminActivity> getActivitiesByAction(String action) {
        return activityRepository.findByActionOrderByTimestampDesc(action);
    }
    
    public List<AdminActivity> getActivitiesByEntityType(String entityType) {
        return activityRepository.findByEntityTypeOrderByTimestampDesc(entityType);
    }
    
    public List<AdminActivity> getActivitiesByDateRange(LocalDateTime start, LocalDateTime end) {
        return activityRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
