package Nhom1.Demo_Nhom1.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import Nhom1.Demo_Nhom1.model.Book;
import Nhom1.Demo_Nhom1.model.Review;
import Nhom1.Demo_Nhom1.service.BookService;

@RestController
@RequestMapping("/api/books")
public class BookApiController {
    
    @Autowired
    private BookService bookService;
    
    @GetMapping
    public ResponseEntity<Page<Book>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookService.findAll(PageRequest.of(page, size)));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        Book book = bookService.findById(id);
        return book != null ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<Book>> searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookService.searchBooks(keyword, PageRequest.of(page, size)));
    }
    
    @PostMapping("/{id}/reviews")
    public ResponseEntity<Book> addReview(@PathVariable Long id, @RequestBody Review review, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        // Set user info
        review.setUserId(authentication.getName());
        review.setUsername(authentication.getName());
        review.setCreatedAt(java.time.LocalDateTime.now());
        
        Book book = bookService.addReview(id, review);
        return book != null ? ResponseEntity.ok(book) : ResponseEntity.notFound().build();
    }
}
