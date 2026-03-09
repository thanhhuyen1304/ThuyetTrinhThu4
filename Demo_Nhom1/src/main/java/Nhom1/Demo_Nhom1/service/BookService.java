package Nhom1.Demo_Nhom1.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Nhom1.Demo_Nhom1.model.Book;
import Nhom1.Demo_Nhom1.model.Review;
import Nhom1.Demo_Nhom1.repository.BookRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookService {
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private SequenceGeneratorService sequenceGenerator;
    
    @Cacheable(value = "books", key = "#id")
    public Book findById(Long id) {
        return bookRepository.findById(id).orElse(null);
    }
    
    public Page<Book> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }
    
    public Page<Book> searchBooks(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bookRepository.findAll(pageable);
        }
        return bookRepository.searchBooks(keyword, pageable);
    }
    
    public Page<Book> findByCategory(String category, Pageable pageable) {
        return bookRepository.findByCategory(category, pageable);
    }
    
    @CacheEvict(value = "books", allEntries = true)
    @Transactional
    public Book save(Book book) {
        if (book.getId() == null) {
            // New book - generate sequential ID
            book.setId(sequenceGenerator.generateSequence(Book.SEQUENCE_NAME));
            book.setCreatedAt(LocalDateTime.now());
        }
        book.setUpdatedAt(LocalDateTime.now());
        return bookRepository.save(book);
    }
    
    @CacheEvict(value = "books", allEntries = true)
    @Transactional
    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }
    
    @Transactional
    public Book addReview(Long bookId, Review review) {
        Book book = findById(bookId);
        if (book != null) {
            book.getReviews().add(review);
            updateAverageRating(book);
            return save(book);
        }
        return null;
    }
    
    private void updateAverageRating(Book book) {
        List<Review> reviews = book.getReviews();
        if (!reviews.isEmpty()) {
            double avg = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            book.setAverageRating(avg);
            book.setTotalReviews(reviews.size());
        }
    }
    
    @CacheEvict(value = "books", key = "#bookId")
    @Transactional
    public boolean updateStock(Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book != null && book.getStockQuantity() >= quantity) {
            book.setStockQuantity(book.getStockQuantity() - quantity);
            book.setUpdatedAt(LocalDateTime.now());
            bookRepository.save(book);
            return true;
        }
        return false;
    }
    
    public long count() {
        return bookRepository.count();
    }
    
    public List<Book> findTopSelling(int limit) {
        // For now, return most recent books
        // In production, this should be based on actual sales data
        return bookRepository.findAll(org.springframework.data.domain.PageRequest.of(0, limit))
                .getContent();
    }
}
