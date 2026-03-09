package Nhom1.Demo_Nhom1.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Nhom1.Demo_Nhom1.model.Order;
import Nhom1.Demo_Nhom1.service.OrderService;

@RestController
@RequestMapping("/public/orders")
public class PublicOrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Public endpoint to retrieve order by id if a valid one-time token is provided.
     * This is used for payment provider redirects where the user's session may be lost.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Order> getPublicOrder(@PathVariable String id, @RequestParam(required = false) String token) {
        System.out.println("=== PublicOrderController.getPublicOrder ===");
        System.out.println("Order ID: " + id);
        System.out.println("Token: " + token);
        
        if (token == null || token.isEmpty()) {
            System.out.println("No token provided!");
            return ResponseEntity.status(403).body(null);
        }

        Order order = orderService.findById(id);
        System.out.println("Order found: " + (order != null ? order.getId() : "null"));
        
        if (order == null) {
            System.out.println("Order not found!");
            return ResponseEntity.notFound().build();
        }

        System.out.println("Stored token: " + order.getViewToken());
        System.out.println("Provided token: " + token);
        
        if (token.equals(order.getViewToken())) {
            System.out.println("Token valid! Clearing token and returning order");
            // Clear token after verification (one-time use)
            order.setViewToken(null);
            order = orderService.save(order);
            return ResponseEntity.ok(order);
        }

        System.out.println("Token invalid!");
        return ResponseEntity.status(403).body(null);
    }
}
