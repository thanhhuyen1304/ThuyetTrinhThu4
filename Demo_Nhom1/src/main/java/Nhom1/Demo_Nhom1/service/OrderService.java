package Nhom1.Demo_Nhom1.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Nhom1.Demo_Nhom1.model.*;
import Nhom1.Demo_Nhom1.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private EmailService emailService;
    
    public Order findById(String id) {
        return orderRepository.findById(id).orElse(null);
    }
    
    /**
     * Find order by ID and verify the viewToken
     * Used for public access after payment redirects
     */
    public Order findByIdAndVerifyToken(String id, String viewToken) {
        if (id == null || viewToken == null) {
            return null;
        }
        Order order = orderRepository.findByIdAndViewToken(id, viewToken);
        if (order != null) {
            // Token verified, clear it so it can't be reused
            order.setViewToken(null);
            orderRepository.save(order);
        }
        return order;
    }
    
    public Page<Order> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
    
    public Page<Order> findByUserId(String userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }
    
    public List<Order> findAllByUserId(String userId) {
        // Sort by createdAt descending (newest first)
        return orderRepository.findByUserId(userId).stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .collect(Collectors.toList());
    }
    
    public Page<Order> findByStatus(String status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }
    
    public Order save(Order order) {
        return orderRepository.save(order);
    }
    
    @Transactional
    public Order createOrder(String userId, String username, String shippingAddress, 
                            String phone, String paymentMethod, String note) {
        return createOrder(userId, username, shippingAddress, phone, paymentMethod, note, null);
    }
    
    @Transactional
    public Order createOrder(String userId, String username, String shippingAddress, 
                            String phone, String paymentMethod, String note,
                            List<String> selectedBookIds) {
        Cart cart = cartService.getCartByUserId(userId);
        
        if (cart.getItems().isEmpty()) {
            return null;
        }
        
        // Lọ items theo selectedBookIds nếu có
        List<CartItem> itemsToOrder;
        if (selectedBookIds != null && !selectedBookIds.isEmpty()) {
            itemsToOrder = cart.getItems().stream()
                .filter(item -> selectedBookIds.contains(String.valueOf(item.getBookId())))
                .collect(Collectors.toList());
            if (itemsToOrder.isEmpty()) {
                return null;
            }
        } else {
            itemsToOrder = cart.getItems();
        }
        
        // Check stock availability
        for (CartItem cartItem : itemsToOrder) {
            Book book = bookService.findById(cartItem.getBookId());
            if (book == null) {
                return null;
            }
        }
        
        Order order = new Order();
        order.setUserId(userId);
        order.setUsername(username);
        order.setShippingAddress(shippingAddress);
        order.setPhone(phone);
        order.setPaymentMethod(paymentMethod);
        order.setNote(note);
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        
        List<OrderItem> orderItems = itemsToOrder.stream()
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setBookId(cartItem.getBookId());
                    orderItem.setBookTitle(cartItem.getBookTitle());
                    orderItem.setImageUrl(cartItem.getImageUrl());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setPrice(cartItem.getPrice());
                    orderItem.setSubtotal(cartItem.getPrice() * cartItem.getQuantity());
                    return orderItem;
                })
                .collect(Collectors.toList());
        
        order.setItems(orderItems);
        double totalAmount = orderItems.stream().mapToDouble(OrderItem::getSubtotal).sum();
        order.setTotalAmount(totalAmount);
        
        // generate one-time view token for external redirects
        order.setViewToken(java.util.UUID.randomUUID().toString());

        Order savedOrder = orderRepository.save(order);
        
        // Clear cart KHÔNG cộng lại stock vì đã chuyển sang order
        cartService.clearCartWithoutRestoreStock(userId);
        
        // Send confirmation email
        try {
            emailService.sendOrderConfirmationEmail(savedOrder, username);
        } catch (Exception e) {
            System.err.println("Failed to send order confirmation email: " + e.getMessage());
        }
        
        return savedOrder;
    }
    
    @Transactional
    public Order updateOrderStatus(String orderId, String status) {
        Order order = findById(orderId);
        if (order != null) {
            String oldStatus = order.getStatus();
            
            // Validate status transition
            if (!isValidStatusTransition(oldStatus, status)) {
                throw new IllegalStateException(
                    "Không thể chuyển trạng thái từ " + getStatusLabel(oldStatus) + 
                    " sang " + getStatusLabel(status)
                );
            }
            
            order.setStatus(status);
            order.setUpdatedAt(LocalDateTime.now());
            
            // Nếu order bị hủy, cộng lại stock
            if ("CANCELLED".equals(status) && !"CANCELLED".equals(oldStatus)) {
                for (OrderItem item : order.getItems()) {
                    Book book = bookService.findById(item.getBookId());
                    if (book != null) {
                        book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
                        bookService.save(book);
                    }
                }
            }
            
            Order updatedOrder = orderRepository.save(order);
            
            // Send status update email
            try {
                emailService.sendOrderStatusUpdateEmail(updatedOrder, order.getUsername(), oldStatus, status);
            } catch (Exception e) {
                System.err.println("Failed to send status update email: " + e.getMessage());
            }
            
            return updatedOrder;
        }
        return null;
    }
    
    /**
     * Kiểm tra xem có thể chuyển từ trạng thái cũ sang trạng thái mới không
     * Quy tắc: Không được phép quay lại trạng thái trước đó
     * Thứ tự: PENDING -> CONFIRMED -> SHIPPED -> DELIVERED
     * CANCELLED có thể từ PENDING hoặc CONFIRMED
     */
    private boolean isValidStatusTransition(String oldStatus, String newStatus) {
        // Nếu giữ nguyên trạng thái thì OK
        if (oldStatus.equals(newStatus)) {
            return true;
        }
        
        // Định nghĩa thứ tự trạng thái (số càng lớn càng về sau)
        int oldLevel = getStatusLevel(oldStatus);
        int newLevel = getStatusLevel(newStatus);
        
        // Trường hợp đặc biệt: CANCELLED
        if ("CANCELLED".equals(newStatus)) {
            // Chỉ có thể hủy khi đơn hàng chưa giao (PENDING hoặc CONFIRMED)
            return "PENDING".equals(oldStatus) || "CONFIRMED".equals(oldStatus);
        }
        
        // Không thể chuyển từ CANCELLED sang trạng thái khác
        if ("CANCELLED".equals(oldStatus)) {
            return false;
        }
        
        // Không thể chuyển từ DELIVERED sang trạng thái khác (trừ giữ nguyên)
        if ("DELIVERED".equals(oldStatus)) {
            return false;
        }
        
        // Chỉ cho phép chuyển sang trạng thái tiếp theo (không được quay lại)
        return newLevel > oldLevel;
    }
    
    /**
     * Trả về level của trạng thái (để so sánh thứ tự)
     */
    private int getStatusLevel(String status) {
        switch (status) {
            case "PENDING": return 1;
            case "CONFIRMED": return 2;
            case "SHIPPED": return 3;
            case "DELIVERED": return 4;
            case "CANCELLED": return 0; // Đặc biệt
            default: return -1;
        }
    }
    
    /**
     * Trả về label tiếng Việt của trạng thái
     */
    private String getStatusLabel(String status) {
        switch (status) {
            case "PENDING": return "Chờ xử lý";
            case "CONFIRMED": return "Đã xác nhận";
            case "SHIPPED": return "Đang giao hàng";
            case "DELIVERED": return "Đã giao hàng";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }
    
    public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findOrdersByDateRange(startDate, endDate);
    }
    
    public long count() {
        return orderRepository.count();
    }
    
    public List<Order> findRecent(int limit) {
        return orderRepository.findAll(
            org.springframework.data.domain.PageRequest.of(0, limit, 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }
}
