package Nhom1.Demo_Nhom1.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import Nhom1.Demo_Nhom1.model.Book;
import Nhom1.Demo_Nhom1.model.Category;
import Nhom1.Demo_Nhom1.model.User;
import Nhom1.Demo_Nhom1.repository.BookRepository;
import Nhom1.Demo_Nhom1.repository.CategoryRepository;
import Nhom1.Demo_Nhom1.repository.UserRepository;
import Nhom1.Demo_Nhom1.service.SequenceGeneratorService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;
    
    @Override
    public void run(String... args) {
        // Clear existing users to avoid duplicates
        userRepository.deleteAll();
        
        // Tạo categories nếu chưa tồn tại
        if (categoryRepository.count() == 0) {
            createSampleCategories();
            System.out.println("✅ Sample categories created");
        }
        
        // Tạo hoặc cập nhật admin user
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@bookstore.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Administrator");
            admin.setPhone("0123456789");
            admin.setAddress("Ha Noi");
            admin.setEnabled(true);
            admin.setCreatedAt(LocalDateTime.now());
        }
        // Đảm bảo admin chỉ có role ADMIN
        Set<String> adminRoles = new HashSet<>();
        adminRoles.add("ADMIN");
        admin.setRoles(adminRoles);
        userRepository.save(admin);
        System.out.println("✅ Admin user created/updated: username=admin, password=admin123, roles=[ADMIN]");
        
        // Tạo tài khoản admin admin1
        User admin1 = userRepository.findByUsername("admin1").orElse(null);
        if (admin1 == null) {
            admin1 = new User();
            admin1.setUsername("admin1");
            admin1.setEmail("admin1@bookstore.com");
            admin1.setPassword(passwordEncoder.encode("admin123"));
            admin1.setFullName("Administrator 1");
            admin1.setPhone("0123456789");
            admin1.setAddress("Ho Chi Minh City");
            admin1.setEnabled(true);
            admin1.setCreatedAt(LocalDateTime.now());
        }
        Set<String> admin1Roles = new HashSet<>();
        admin1Roles.add("ADMIN");
        admin1.setRoles(admin1Roles);
        userRepository.save(admin1);
        System.out.println("✅ Admin user created/updated: username=admin1, password=admin123, roles=[ADMIN]");
        
        // Tạo tài khoản admin admin2
        User admin2 = userRepository.findByUsername("admin2").orElse(null);
        if (admin2 == null) {
            admin2 = new User();
            admin2.setUsername("admin2");
            admin2.setEmail("admin2@bookstore.com");
            admin2.setPassword(passwordEncoder.encode("admin123"));
            admin2.setFullName("Administrator 2");
            admin2.setPhone("0123456789");
            admin2.setAddress("Da Nang");
            admin2.setEnabled(true);
            admin2.setCreatedAt(LocalDateTime.now());
        }
        Set<String> admin2Roles = new HashSet<>();
        admin2Roles.add("ADMIN");
        admin2.setRoles(admin2Roles);
        userRepository.save(admin2);
        System.out.println("✅ Admin user created/updated: username=admin2, password=admin123, roles=[ADMIN]");
        
        // Tạo hoặc cập nhật user thường
        User user = userRepository.findByUsername("user").orElse(null);
        if (user == null) {
            user = new User();
            user.setUsername("user");
            user.setEmail("user@bookstore.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setFullName("Test User");
            user.setPhone("0987654321");
            user.setAddress("Ho Chi Minh");
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
        }
        // Đảm bảo user có đúng role
        Set<String> userRoles = new HashSet<>();
        userRoles.add("USER");
        user.setRoles(userRoles);
        userRepository.save(user);
        System.out.println("✅ Test user created/updated: username=user, password=user123, roles=[USER]");
        
        // Tạo một số sách mẫu nếu database trống
        if (bookRepository.count() == 0) {
            createSampleBooks();
            System.out.println("✅ Sample books created");
        }
    }
    
    private void createSampleBooks() {
        Book[] books = {
            createBook("Clean Code", "Robert C. Martin", "Programming", 
                "A Handbook of Agile Software Craftsmanship", 
                "Prentice Hall", 2008, 450000.0, 50,
                "/uploads/books/09b32a0f-4916-42e6-a4b6-1986148f5784.png"),
            
            createBook("Design Patterns", "Gang of Four", "Programming",
                "Elements of Reusable Object-Oriented Software",
                "Addison-Wesley", 1994, 520000.0, 30,
                "/uploads/books/3e9b081c-2e81-4f02-b12b-61b2c97d6b5f.jpg"),
            
            createBook("The Pragmatic Programmer", "Andrew Hunt", "Programming",
                "Your Journey To Mastery",
                "Addison-Wesley", 2019, 480000.0, 40,
                "/uploads/books/df280018-cc51-4528-afe8-7fc0366ca45c.png"),
            
            createBook("Introduction to Algorithms", "Thomas H. Cormen", "Computer Science",
                "A comprehensive textbook on algorithms",
                "MIT Press", 2009, 850000.0, 25,
                "https://via.placeholder.com/300x400?text=Introduction+to+Algorithms"),
            
            createBook("Effective Java", "Joshua Bloch", "Programming",
                "Best Practices for the Java Platform",
                "Addison-Wesley", 2018, 550000.0, 35,
                "https://via.placeholder.com/300x400?text=Effective+Java"),
            
            createBook("Head First Design Patterns", "Eric Freeman", "Programming",
                "A Brain-Friendly Guide",
                "O'Reilly Media", 2004, 420000.0, 45,
                "https://via.placeholder.com/300x400?text=Head+First+Design+Patterns")
        };
        
        for (Book book : books) {
            bookRepository.save(book);
        }
    }
    
    private Book createBook(String title, String author, String category, String description,
                           String publisher, int year, double price, int stock, String imageUrl) {
        Book book = new Book();
        book.setId(sequenceGeneratorService.generateSequence(Book.SEQUENCE_NAME));
        book.setTitle(title);
        book.setAuthor(author);
        book.setCategory(category);
        book.setDescription(description);
        book.setPublisher(publisher);
        book.setPublishYear(year);
        book.setPrice(price);
        book.setStockQuantity(stock);
        book.setImageUrl(imageUrl);
        book.setAverageRating(4.5);
        book.setTotalReviews(0);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        return book;
    }
    
    private void createSampleCategories() {
        String[] categories = {
            "Programming", "Computer Science", "Web Development", 
            "Mobile Development", "Data Science", "Artificial Intelligence",
            "Software Engineering", "Database", "Networking", "Security"
        };
        
        String[] descriptions = {
            "Books about programming languages and coding",
            "Computer science fundamentals and theory",
            "Web development frameworks and technologies",
            "Mobile app development for iOS and Android",
            "Data analysis, machine learning, and statistics",
            "AI, machine learning, and deep learning",
            "Software design patterns and best practices",
            "Database design and management",
            "Computer networking and protocols",
            "Cybersecurity and information security"
        };
        
        for (int i = 0; i < categories.length; i++) {
            Category category = new Category();
            category.setName(categories[i]);
            category.setDescription(descriptions[i]);
            category.setCreatedAt(LocalDateTime.now());
            categoryRepository.save(category);
        }
    }
}
