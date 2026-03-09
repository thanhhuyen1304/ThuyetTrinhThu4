package Nhom1.Demo_Nhom1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "books")
public class Book {
    @Transient
    public static final String SEQUENCE_NAME = "books_sequence";
    
    @Id
    private Long id;
    
    @Indexed
    private String title;
    
    private String author;
    private String category;
    private String description;
    private String publisher;
    private Integer publishYear;
    private Double price;
    private Integer stockQuantity;
    private String imageUrl; // Ảnh chính
    private List<String> additionalImages = new ArrayList<>(); // Ảnh phụ
    private Double averageRating = 0.0;
    private Integer totalReviews = 0;
    private List<Review> reviews = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
