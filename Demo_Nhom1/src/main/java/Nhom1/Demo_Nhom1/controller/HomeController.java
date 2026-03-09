package Nhom1.Demo_Nhom1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import Nhom1.Demo_Nhom1.model.Book;
import Nhom1.Demo_Nhom1.model.User;
import Nhom1.Demo_Nhom1.service.BookService;
import Nhom1.Demo_Nhom1.service.UserService;

import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class HomeController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private UserService userService;
    
    @ModelAttribute
    public void addUserToModel(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            model.addAttribute("currentUser", user);
        }
    }
    
    @GetMapping({"/", "/home"})
    public String home(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Model model) {
        
        Page<Book> bookPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            bookPage = bookService.searchBooks(keyword, PageRequest.of(page, size));
            model.addAttribute("keyword", keyword);
        } else if (category != null && !category.trim().isEmpty()) {
            bookPage = bookService.findByCategory(category, PageRequest.of(page, size));
            model.addAttribute("category", category);
        } else {
            bookPage = bookService.findAll(PageRequest.of(page, size));
        }
        
        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("totalItems", bookPage.getTotalElements());
        
        return "home";
    }
    
    // Redirect /books to home page
    @GetMapping("/books")
    public String booksListing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Model model) {
        
        System.out.println("========================================");
        System.out.println("DEBUG /books endpoint");
        System.out.println("Page: " + page + ", Size: " + size);
        System.out.println("Keyword: " + keyword + ", Category: " + category);
        
        Page<Book> bookPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            bookPage = bookService.searchBooks(keyword, PageRequest.of(page, size));
            model.addAttribute("keyword", keyword);
        } else if (category != null && !category.trim().isEmpty()) {
            bookPage = bookService.findByCategory(category, PageRequest.of(page, size));
            model.addAttribute("category", category);
        } else {
            bookPage = bookService.findAll(PageRequest.of(page, size));
        }
        
        System.out.println("Total books in database: " + bookPage.getTotalElements());
        System.out.println("Books in current page: " + bookPage.getContent().size());
        
        // Get all unique categories for filter
        List<String> categories = bookService.findAll(PageRequest.of(0, 1000)).getContent()
                .stream()
                .map(Book::getCategory)
                .filter(cat -> cat != null && !cat.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        
        System.out.println("Categories found: " + categories.size());
        System.out.println("Categories: " + categories);
        
        if (!bookPage.getContent().isEmpty()) {
            System.out.println("First book: " + bookPage.getContent().get(0).getTitle());
        } else {
            System.out.println("WARNING: No books found!");
        }
        
        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("totalItems", bookPage.getTotalElements());
        
        System.out.println("Model attributes set successfully");
        System.out.println("Returning template: books");
        System.out.println("========================================");
        
        return "books";
    }
    
    @GetMapping("/books/detail")
    public String bookDetail(@RequestParam Long id, Model model) {
        System.out.println("========================================");
        System.out.println("Book Detail - ID: " + id);
        
        Book book = bookService.findById(id);
        if (book == null) {
            System.out.println("ERROR: Book not found with ID: " + id);
            return "redirect:/books?error=Không+tìm+thấy+sách";
        }
        
        System.out.println("Book found: " + book.getTitle());
        System.out.println("Book ID: " + book.getId());
        System.out.println("========================================");
        
        model.addAttribute("book", book);
        return "book-detail";
    }
    
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "profile";
    }
    
    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }
    
    @GetMapping("/checkout")
    public String checkout() {
        return "checkout";
    }
    
    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }
    
    @GetMapping("/orders/delivered")
    public String deliveredOrders() {
        return "delivered-orders";
    }
    
    @GetMapping("/orders/detail")
    public String orderDetail(
            @RequestParam String id,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String payment,
            Authentication authentication,
            Model model) {
        
        System.out.println("=== HomeController.orderDetail ===");
        System.out.println("Order ID: " + id);
        System.out.println("Token: " + token);
        System.out.println("Payment: " + payment);
        
        // Pass parameters to template - template will handle fetching via API
        model.addAttribute("orderId", id);
        model.addAttribute("token", token);
        model.addAttribute("payment", payment);
        
        return "order-detail";
    }
    
    @GetMapping("/test-books")
    public String testBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        
        System.out.println("========================================");
        System.out.println("TEST ENDPOINT /test-books");
        
        Page<Book> bookPage = bookService.findAll(PageRequest.of(page, size));
        List<String> categories = bookService.findAll(PageRequest.of(0, 1000)).getContent()
                .stream()
                .map(Book::getCategory)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        
        System.out.println("Total books: " + bookPage.getTotalElements());
        System.out.println("Books in page: " + bookPage.getContent().size());
        System.out.println("Categories: " + categories.size());
        
        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("categories", categories);
        model.addAttribute("totalItems", bookPage.getTotalElements());
        
        System.out.println("Returning test-simple template");
        System.out.println("========================================");
        
        return "test-simple";
    }
}
