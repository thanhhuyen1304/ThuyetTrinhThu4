package Nhom1.Demo_Nhom1.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Nhom1.Demo_Nhom1.model.Book;
import Nhom1.Demo_Nhom1.model.Cart;
import Nhom1.Demo_Nhom1.model.CartItem;
import Nhom1.Demo_Nhom1.repository.CartRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CartService {
    
    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private BookService bookService;
    
    public Cart getCartByUserId(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    return cartRepository.save(cart);
                });
    }
    
    @Transactional
    public Cart addToCart(String userId, Long bookId, int quantity) {
        Cart cart = getCartByUserId(userId);
        Book book = bookService.findById(bookId);
        
        if (book == null || book.getStockQuantity() < quantity) {
            return null;
        }
        
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getBookId().equals(bookId))
                .findFirst();
        
        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setBookId(bookId);
            newItem.setBookTitle(book.getTitle());
            newItem.setQuantity(quantity);
            newItem.setPrice(book.getPrice());
            newItem.setImageUrl(book.getImageUrl());
            cart.getItems().add(newItem);
        }
        
        // Trừ stock khi thêm vào giỏ
        bookService.updateStock(bookId, quantity);
        
        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
    
    @Transactional
    public Cart removeFromCart(String userId, Long bookId) {
        Cart cart = getCartByUserId(userId);
        
        // Tìm item để lấy quantity trước khi xóa
        Optional<CartItem> itemToRemove = cart.getItems().stream()
                .filter(item -> item.getBookId().equals(bookId))
                .findFirst();
        
        if (itemToRemove.isPresent()) {
            // Cộng lại stock khi xóa khỏi giỏ
            int quantityToRestore = itemToRemove.get().getQuantity();
            Book book = bookService.findById(bookId);
            if (book != null) {
                book.setStockQuantity(book.getStockQuantity() + quantityToRestore);
                bookService.save(book);
            }
        }
        
        cart.getItems().removeIf(item -> item.getBookId().equals(bookId));
        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
    
    @Transactional
    public Cart updateQuantity(String userId, Long bookId, int newQuantity) {
        Cart cart = getCartByUserId(userId);
        Book book = bookService.findById(bookId);
        
        if (book == null) {
            return cart;
        }
        
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getBookId().equals(bookId))
                .findFirst();
        
        if (existingItem.isPresent()) {
            int oldQuantity = existingItem.get().getQuantity();
            int difference = newQuantity - oldQuantity;
            
            // Kiểm tra stock có đủ không
            if (difference > 0 && book.getStockQuantity() < difference) {
                return null; // Không đủ stock
            }
            
            // Cập nhật stock: nếu tăng quantity thì trừ stock, nếu giảm thì cộng lại
            if (difference > 0) {
                bookService.updateStock(bookId, difference);
            } else if (difference < 0) {
                book.setStockQuantity(book.getStockQuantity() + Math.abs(difference));
                bookService.save(book);
            }
            
            existingItem.get().setQuantity(newQuantity);
        }
        
        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
    
    @Transactional
    public void clearCart(String userId) {
        Cart cart = getCartByUserId(userId);
        
        // Cộng lại stock cho tất cả items trong giỏ
        for (CartItem item : cart.getItems()) {
            Book book = bookService.findById(item.getBookId());
            if (book != null) {
                book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
                bookService.save(book);
            }
        }
        
        cart.getItems().clear();
        cart.setTotalAmount(0.0);
        cartRepository.save(cart);
    }
    
    @Transactional
    public void clearCartWithoutRestoreStock(String userId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().clear();
        cart.setTotalAmount(0.0);
        cartRepository.save(cart);
    }
    
    public Cart save(Cart cart) {
        return cartRepository.save(cart);
    }
    
    private void updateCartTotal(Cart cart) {
        double total = cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        cart.setTotalAmount(total);
    }
}
