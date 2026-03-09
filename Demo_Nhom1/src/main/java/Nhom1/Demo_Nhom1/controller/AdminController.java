package Nhom1.Demo_Nhom1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import Nhom1.Demo_Nhom1.model.Book;
import Nhom1.Demo_Nhom1.model.Category;
import Nhom1.Demo_Nhom1.model.Order;
import Nhom1.Demo_Nhom1.model.User;
import Nhom1.Demo_Nhom1.service.BookService;
import Nhom1.Demo_Nhom1.service.CategoryService;
import Nhom1.Demo_Nhom1.service.FileStorageService;
import Nhom1.Demo_Nhom1.service.OrderService;
import Nhom1.Demo_Nhom1.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private Nhom1.Demo_Nhom1.repository.BookRepository bookRepository;
    
    @Autowired
    private Nhom1.Demo_Nhom1.repository.UserRepository userRepository;
    
    @Autowired
    private Nhom1.Demo_Nhom1.repository.CartRepository cartRepository;
    
    @Autowired
    private Nhom1.Demo_Nhom1.repository.OrderRepository orderRepository;
    
    @Autowired
    private Nhom1.Demo_Nhom1.repository.CategoryRepository categoryRepository;
    
    @Autowired
    private Nhom1.Demo_Nhom1.service.AdminActivityService adminActivityService;
    
    @Autowired
    private Nhom1.Demo_Nhom1.service.CartService cartService;
    
    @ModelAttribute
    public void addUserToModel(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            model.addAttribute("currentUser", user);
        }
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, jakarta.servlet.http.HttpServletRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        // Monthly statistics
        List<Order> monthlyOrders = orderService.getOrdersByDateRange(startOfMonth, now);
        double monthlyRevenue = monthlyOrders.stream()
                .filter(o -> "DELIVERED".equals(o.getStatus()))
                .mapToDouble(Order::getTotalAmount)
                .sum();
        
        // Total statistics
        long totalBooks = bookService.count();
        long totalUsers = userService.count();
        long totalOrders = orderService.count();
        
        // Top selling books
        List<Book> topBooks = bookService.findTopSelling(5);
        
        // Recent orders
        List<Order> recentOrders = orderService.findRecent(5);
        
        // Revenue by month (last 6 months)
        List<Double> monthlyRevenueData = new java.util.ArrayList<>();
        List<String> monthLabels = new java.util.ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            List<Order> orders = orderService.getOrdersByDateRange(monthStart, monthEnd);
            double revenue = orders.stream()
                    .filter(o -> "DELIVERED".equals(o.getStatus()))
                    .mapToDouble(Order::getTotalAmount)
                    .sum();
            monthlyRevenueData.add(revenue);
            monthLabels.add(monthStart.getMonth().toString().substring(0, 3));
        }
        
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("topBooks", topBooks);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("monthlyRevenueData", monthlyRevenueData);
        model.addAttribute("monthLabels", monthLabels);
        
        return "admin/dashboard";
    }
    
    @GetMapping("/tools")
    public String tools() {
        return "admin/tools";
    }
    
    @GetMapping("/books")
    public String manageBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        Page<Book> bookPage = bookService.findAll(PageRequest.of(page, size));
        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        
        // Calculate statistics
        long totalBooks = bookService.count();
        long inStockBooks = bookRepository.findAll().stream()
            .filter(b -> b.getStockQuantity() != null && b.getStockQuantity() > 0)
            .count();
        long outOfStockBooks = bookRepository.findAll().stream()
            .filter(b -> b.getStockQuantity() == null || b.getStockQuantity() == 0)
            .count();
        double totalValue = bookRepository.findAll().stream()
            .filter(b -> b.getPrice() != null && b.getStockQuantity() != null)
            .mapToDouble(b -> b.getPrice() * b.getStockQuantity())
            .sum();
        
        model.addAttribute("totalBooks", totalBooks);
        model.addAttribute("inStockBooks", inStockBooks);
        model.addAttribute("outOfStockBooks", outOfStockBooks);
        model.addAttribute("totalValue", totalValue);
        
        return "admin/books";
    }
    
    @GetMapping("/books/add")
    public String addBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/book-form";
    }
    
    @PostMapping("/books/save")
        public String saveBook(@ModelAttribute Book book, 
                              @RequestParam(required = false) String additionalImages,
                              Model model, 
                              jakarta.servlet.http.HttpServletRequest request) {
            try {
                boolean isNew = (book.getId() == null);

                System.out.println("=== SAVE BOOK DEBUG ===");
                System.out.println("Book ID: " + book.getId());
                System.out.println("additionalImages param: " + additionalImages);

                Book existingBook = null;

                // Nếu đang chỉnh sửa (có id), load sách hiện có để giữ lại reviews
                if (book.getId() != null) {
                    existingBook = bookService.findById(book.getId());
                    if (existingBook != null) {
                        System.out.println("Existing book additionalImages: " + existingBook.getAdditionalImages());

                        book.setReviews(existingBook.getReviews() != null ? existingBook.getReviews() : new java.util.ArrayList<>());
                        book.setAverageRating(existingBook.getAverageRating());
                        book.setTotalReviews(existingBook.getTotalReviews());
                        book.setCreatedAt(existingBook.getCreatedAt());
                        // Preserve existing image if no new image URL is provided
                        if (book.getImageUrl() == null || book.getImageUrl().isEmpty()) {
                            book.setImageUrl(existingBook.getImageUrl());
                        }
                    }
                } else {
                    book.setCreatedAt(LocalDateTime.now());
                }
                book.setUpdatedAt(LocalDateTime.now());

                // Parse additional images from comma-separated string
                if (additionalImages != null && !additionalImages.isEmpty()) {
                    String[] imageArray = additionalImages.split(",");
                    List<String> newImageList = new java.util.ArrayList<>();

                    // Clean and add new images
                    for (String img : imageArray) {
                        String trimmed = img.trim();
                        if (!trimmed.isEmpty()) {
                            newImageList.add(trimmed);
                        }
                    }

                    System.out.println("New images from param: " + newImageList);

                    // If editing, merge with existing additional images
                    if (existingBook != null && existingBook.getAdditionalImages() != null) {
                        System.out.println("Merging with existing images...");
                        // Add existing images that are not in the new list
                        for (String existingImg : existingBook.getAdditionalImages()) {
                            if (!newImageList.contains(existingImg)) {
                                newImageList.add(existingImg);
                            }
                        }
                    }

                    book.setAdditionalImages(newImageList);
                    System.out.println("Final additionalImages: " + newImageList);
                } else {
                    // No new images in param
                    if (existingBook != null && existingBook.getAdditionalImages() != null) {
                        // Keep existing images
                        book.setAdditionalImages(existingBook.getAdditionalImages());
                        System.out.println("Keeping existing images: " + existingBook.getAdditionalImages());
                    } else if (book.getAdditionalImages() == null) {
                        book.setAdditionalImages(new java.util.ArrayList<>());
                        System.out.println("No images, setting empty list");
                    }
                }

                // Khởi tạo giá trị mặc định nếu null
                if (book.getAverageRating() == null) {
                    book.setAverageRating(0.0);
                }
                if (book.getTotalReviews() == null) {
                    book.setTotalReviews(0);
                }
                if (book.getReviews() == null) {
                    book.setReviews(new java.util.ArrayList<>());
                }

                System.out.println("Saving book: " + book.getTitle() + " with image URL: " + book.getImageUrl() + 
                                 " and " + (book.getAdditionalImages() != null ? book.getAdditionalImages().size() : 0) + " additional images");
                System.out.println("Additional images list: " + book.getAdditionalImages());

                Book savedBook = bookService.save(book);

                System.out.println("Book saved successfully!");
                System.out.println("======================");

                // Log activity với thông tin chi tiết
                if (isNew) {
                    String description = "Thêm sách mới: \"" + book.getTitle() + "\" - Tác giả: " + book.getAuthor() + 
                        ", Giá: " + String.format("%,.0f", book.getPrice()) + " VND, Số lượng: " + book.getStockQuantity() +
                        (book.getAdditionalImages() != null && book.getAdditionalImages().size() > 0 ? 
                            ", Ảnh phụ: " + book.getAdditionalImages().size() : "");
                    adminActivityService.logActivity("CREATE", "BOOK", 
                        savedBook.getId() != null ? savedBook.getId().toString() : null, 
                        book.getTitle(), description, request);
                } else {
                    // For UPDATE, track what changed
                    StringBuilder changes = new StringBuilder();
                    StringBuilder oldValues = new StringBuilder();
                    StringBuilder newValues = new StringBuilder();
                    
                    if (existingBook != null) {
                        if (!existingBook.getTitle().equals(book.getTitle())) {
                            changes.append("Tiêu đề, ");
                            oldValues.append("Tiêu đề: ").append(existingBook.getTitle()).append("\n");
                            newValues.append("Tiêu đề: ").append(book.getTitle()).append("\n");
                        }
                        if (!existingBook.getAuthor().equals(book.getAuthor())) {
                            changes.append("Tác giả, ");
                            oldValues.append("Tác giả: ").append(existingBook.getAuthor()).append("\n");
                            newValues.append("Tác giả: ").append(book.getAuthor()).append("\n");
                        }
                        if (!existingBook.getPrice().equals(book.getPrice())) {
                            changes.append("Giá, ");
                            oldValues.append("Giá: ").append(String.format("%,.0f", existingBook.getPrice())).append(" VND\n");
                            newValues.append("Giá: ").append(String.format("%,.0f", book.getPrice())).append(" VND\n");
                        }
                        if (!existingBook.getStockQuantity().equals(book.getStockQuantity())) {
                            changes.append("Số lượng, ");
                            oldValues.append("Số lượng: ").append(existingBook.getStockQuantity()).append("\n");
                            newValues.append("Số lượng: ").append(book.getStockQuantity()).append("\n");
                        }
                        if (existingBook.getDescription() != null && book.getDescription() != null && 
                            !existingBook.getDescription().equals(book.getDescription())) {
                            changes.append("Mô tả, ");
                            oldValues.append("Mô tả: ").append(existingBook.getDescription().length() > 100 ? 
                                existingBook.getDescription().substring(0, 100) + "..." : existingBook.getDescription()).append("\n");
                            newValues.append("Mô tả: ").append(book.getDescription().length() > 100 ? 
                                book.getDescription().substring(0, 100) + "..." : book.getDescription()).append("\n");
                        }
                        if (existingBook.getImageUrl() != null && book.getImageUrl() != null && 
                            !existingBook.getImageUrl().equals(book.getImageUrl())) {
                            changes.append("Ảnh chính, ");
                        }
                    }
                    
                    String changesStr = changes.length() > 0 ? changes.substring(0, changes.length() - 2) : "Không có thay đổi";
                    String description = "Cập nhật sách: \"" + book.getTitle() + "\" - Thay đổi: " + changesStr;
                    
                    adminActivityService.logActivityWithChanges("UPDATE", "BOOK", 
                        savedBook.getId() != null ? savedBook.getId().toString() : null, 
                        book.getTitle(), description, 
                        oldValues.length() > 0 ? oldValues.toString().trim() : null,
                        newValues.length() > 0 ? newValues.toString().trim() : null,
                        request);
                }

                return "redirect:/admin/books";
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("error", "Lỗi khi lưu sách: " + e.getMessage());
                model.addAttribute("book", book);
                model.addAttribute("categories", categoryService.findAll());
                return "admin/book-form";
            }
        }

    
    @PostMapping("/books/upload-image-ajax")
    @ResponseBody
    public java.util.Map<String, String> uploadImageAjax(@RequestParam("imageFile") MultipartFile imageFile) {
        java.util.Map<String, String> response = new java.util.HashMap<>();
        try {
            System.out.println("Received image upload request. File name: " + imageFile.getOriginalFilename() 
                    + ", Size: " + imageFile.getSize() + " bytes, Content type: " + imageFile.getContentType());
            
            if (imageFile == null || imageFile.isEmpty()) {
                response.put("success", "false");
                response.put("error", "No file provided");
                System.out.println("Upload failed: No file provided");
                return response;
            }
            
            // Validate file type
            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", "false");
                response.put("error", "Invalid file type. Please upload an image file.");
                System.out.println("Upload failed: Invalid file type: " + contentType);
                return response;
            }
            
            // Validate file size (5MB max for safety)
            long maxSize = 5 * 1024 * 1024;
            if (imageFile.getSize() > maxSize) {
                response.put("success", "false");
                response.put("error", "File size exceeds 5MB limit");
                System.out.println("Upload failed: File too large: " + imageFile.getSize() + " bytes");
                return response;
            }
            
            String imageUrl = fileStorageService.storeFile(imageFile);
            response.put("success", "true");
            response.put("imageUrl", imageUrl);
            System.out.println("Upload successful. Image URL: " + imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", "false");
            response.put("error", "Upload failed: " + e.getMessage());
            System.out.println("Upload exception: " + e.getMessage());
        }
        return response;
    }
    
    @PostMapping("/books/reset-ids")
    @ResponseBody
    public java.util.Map<String, String> resetBookIds() {
        java.util.Map<String, String> response = new java.util.HashMap<>();
        try {
            // Get all books
            List<Book> allBooks = bookService.findAll(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
            
            // Delete all books
            bookRepository.deleteAll();
            
            // Re-create with numeric IDs
            int counter = 1;
            for (Book oldBook : allBooks) {
                Book newBook = new Book();
                newBook.setId((long) counter++);
                newBook.setTitle(oldBook.getTitle());
                newBook.setAuthor(oldBook.getAuthor());
                newBook.setCategory(oldBook.getCategory());
                newBook.setDescription(oldBook.getDescription());
                newBook.setPublisher(oldBook.getPublisher());
                newBook.setPublishYear(oldBook.getPublishYear());
                newBook.setPrice(oldBook.getPrice());
                newBook.setStockQuantity(oldBook.getStockQuantity());
                newBook.setImageUrl(oldBook.getImageUrl());
                newBook.setAverageRating(oldBook.getAverageRating());
                newBook.setTotalReviews(oldBook.getTotalReviews());
                newBook.setReviews(oldBook.getReviews());
                newBook.setCreatedAt(oldBook.getCreatedAt());
                newBook.setUpdatedAt(LocalDateTime.now());
                bookRepository.save(newBook);
            }
            
            response.put("success", "true");
            response.put("message", "Reset " + allBooks.size() + " books with numeric IDs");
        } catch (Exception e) {
            response.put("success", "false");
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    @GetMapping("/fix-admin-role")
    @ResponseBody
    public String fixAdminRole() {
        try {
            User admin = userService.findByUsername("admin");
            if (admin != null) {
                java.util.Set<String> roles = new java.util.HashSet<>();
                roles.add("ADMIN");
                roles.add("USER");
                admin.setRoles(roles);
                userRepository.save(admin);
                return "Admin role fixed! Please logout and login again.";
            }
            return "Admin user not found!";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    @GetMapping("/books/reset-ids-get")
    public String resetBookIdsGet() {
        try {
            // Get all books
            List<Book> allBooks = bookService.findAll(org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
            
            // Delete all books
            bookRepository.deleteAll();
            
            // Re-create with numeric IDs
            int counter = 1;
            for (Book oldBook : allBooks) {
                Book newBook = new Book();
                newBook.setId((long) counter++);
                newBook.setTitle(oldBook.getTitle());
                newBook.setAuthor(oldBook.getAuthor());
                newBook.setCategory(oldBook.getCategory());
                newBook.setDescription(oldBook.getDescription());
                newBook.setPublisher(oldBook.getPublisher());
                newBook.setPublishYear(oldBook.getPublishYear());
                newBook.setPrice(oldBook.getPrice());
                newBook.setStockQuantity(oldBook.getStockQuantity());
                newBook.setImageUrl(oldBook.getImageUrl());
                newBook.setAverageRating(oldBook.getAverageRating());
                newBook.setTotalReviews(oldBook.getTotalReviews());
                newBook.setReviews(oldBook.getReviews());
                newBook.setCreatedAt(oldBook.getCreatedAt());
                newBook.setUpdatedAt(LocalDateTime.now());
                bookRepository.save(newBook);
            }
            
            return "redirect:/admin/books?success=Reset " + allBooks.size() + " books with numeric IDs (1, 2, 3...)";
        } catch (Exception e) {
            return "redirect:/admin/books?error=" + e.getMessage();
        }
    }
    
    @GetMapping("/books/upload-image")
    public String uploadImageForm(@RequestParam Long id, Model model) {
        Book book = bookService.findById(id);
        model.addAttribute("book", book);
        return "admin/book-image-upload";
    }
    
    @PostMapping("/books/upload-image")
    public String uploadImage(@RequestParam Long id,
                             @RequestParam("imageFile") MultipartFile imageFile,
                             Model model) {
        try {
            Book book = bookService.findById(id);
            if (book == null) {
                model.addAttribute("error", "Không tìm thấy sách");
                return "redirect:/admin/books";
            }
            
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = fileStorageService.storeFile(imageFile);
                book.setImageUrl(imageUrl);
                bookService.save(book);
            }
            
            return "redirect:/admin/books";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Lỗi khi tải ảnh lên: " + e.getMessage());
            Book book = bookService.findById(id);
            model.addAttribute("book", book);
            return "admin/book-image-upload";
        }
    }
    
    @GetMapping("/books/edit")
    public String editBookForm(@RequestParam Long id, Model model) {
        Book book = bookService.findById(id);
        if (book == null) {
            return "redirect:/admin/books?error=Không+tìm+thấy+sách";
        }
        model.addAttribute("book", book);
        model.addAttribute("categories", categoryService.findAll());
        return "admin/book-form";
    }
    
    @GetMapping("/books/edit/{id}")
    public String editBookFormByPath(@PathVariable String id, Model model) {
        try {
            Long bookId = Long.parseLong(id);
            Book book = bookService.findById(bookId);
            if (book == null) {
                return "redirect:/admin/books?error=Không+tìm+thấy+sách";
            }
            model.addAttribute("book", book);
            model.addAttribute("categories", categoryService.findAll());
            return "admin/book-form";
        } catch (NumberFormatException e) {
            return "redirect:/admin/books?error=ID+không+hợp+lệ";
        }
    }
    
    @GetMapping("/books/delete")
    public String deleteBook(@RequestParam Long id, jakarta.servlet.http.HttpServletRequest request) {
        Book book = bookService.findById(id);
        String bookTitle = book != null ? book.getTitle() : "Unknown";
        String bookInfo = book != null ? 
            " - Tác giả: " + book.getAuthor() + ", Giá: " + String.format("%,.0f", book.getPrice()) + " VND" : "";
        
        bookService.deleteById(id);
        
        // Log activity
        adminActivityService.logActivity("DELETE", "BOOK", id.toString(), bookTitle, 
            "Xóa sách: \"" + bookTitle + "\"" + bookInfo, request);
        
        return "redirect:/admin/books";
    }
    
    @GetMapping("/orders")
    public String manageOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        Page<Order> orderPage = orderService.findAll(PageRequest.of(page, size));
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orderPage.getTotalPages());
        
        // Calculate statistics
        List<Order> allOrders = orderRepository.findAll();
        long totalOrders = allOrders.size();
        long pendingOrders = allOrders.stream()
            .filter(o -> "PENDING".equals(o.getStatus()))
            .count();
        long deliveredOrders = allOrders.stream()
            .filter(o -> "DELIVERED".equals(o.getStatus()))
            .count();
        double totalRevenue = allOrders.stream()
            .filter(o -> "DELIVERED".equals(o.getStatus()))
            .mapToDouble(Order::getTotalAmount)
            .sum();
        
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("deliveredOrders", deliveredOrders);
        model.addAttribute("totalRevenue", totalRevenue);
        
        return "admin/orders";
    }
    
    @PostMapping("/orders/update-status")
    public String updateOrderStatus(@RequestParam String orderId, 
                                   @RequestParam String status,
                                   Model model,
                                   jakarta.servlet.http.HttpServletRequest request) {
        try {
            System.out.println("UPDATE STATUS - orderId: " + orderId + ", new status: " + status);
            
            if (orderId == null || orderId.trim().isEmpty()) {
                System.out.println("ERROR: orderId is empty");
                return "redirect:/admin/orders?error=Không+tìm+thấy+ID+đơn+hàng";
            }
            
            if (status == null || status.trim().isEmpty()) {
                System.out.println("ERROR: status is empty");
                return "redirect:/admin/orders?error=Vui+lòng+chọn+trạng+thái";
            }
            
            // Get old status before update
            Order existingOrder = orderService.findById(orderId);
            String oldStatus = existingOrder != null ? existingOrder.getStatus() : "Unknown";
            
            Order order = orderService.updateOrderStatus(orderId, status);
            
            // Log activity với thông tin đơn hàng và thay đổi trạng thái
            String orderInfo = order != null ? 
                " - Khách hàng: " + order.getUsername() + 
                ", Tổng tiền: " + String.format("%,.0f", order.getTotalAmount()) + " VND" : "";
            String description = "Cập nhật trạng thái đơn hàng từ \"" + oldStatus + "\" thành \"" + status + "\"" + orderInfo;
            
            adminActivityService.logActivityWithChanges("UPDATE", "ORDER", orderId, 
                "Order #" + orderId.substring(0, Math.min(8, orderId.length())), 
                description,
                "Trạng thái: " + oldStatus,
                "Trạng thái: " + status,
                request);
            
            System.out.println("SUCCESS: Order " + orderId + " updated to status: " + status);
            return "redirect:/admin/orders?updated=true";
        } catch (IllegalStateException e) {
            System.out.println("ILLEGAL STATE: " + e.getMessage());
            return "redirect:/admin/orders?error=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/orders?error=Có+lỗi+xảy+ra:+" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    
    @GetMapping("/users")
    public String manageUsers(Model model) {
        List<User> users = userService.findAll();
        
        System.out.println("=== DEBUG: Total users found: " + users.size());
        users.forEach(u -> System.out.println("  - " + u.getUsername() + " (" + u.getRoles() + ")"));
        
        // Calculate statistics
        long totalUsers = users.size();
        long adminCount = users.stream().filter(u -> u.getRoles().contains("ADMIN")).count();
        long userCount = users.stream().filter(u -> u.getRoles().contains("USER") && !u.getRoles().contains("ADMIN")).count();
        long activeCount = users.stream().filter(User::isEnabled).count();
        
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("userCount", userCount);
        model.addAttribute("activeCount", activeCount);
        
        return "admin/users-simple";
    }
    
    // Category Management
    @GetMapping("/categories")
    public String manageCategories(Model model) {
        List<Category> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        
        // Calculate statistics
        long totalCategories = categories.size();
        long categoriesWithBooks = categories.stream()
            .filter(c -> {
                long bookCount = bookRepository.findAll().stream()
                    .filter(b -> b.getCategory() != null && b.getCategory().equals(c.getName()))
                    .count();
                return bookCount > 0;
            })
            .count();
        long emptyCategories = totalCategories - categoriesWithBooks;
        long totalBooksInCategories = bookRepository.count();
        
        model.addAttribute("totalCategories", totalCategories);
        model.addAttribute("categoriesWithBooks", categoriesWithBooks);
        model.addAttribute("emptyCategories", emptyCategories);
        model.addAttribute("totalBooksInCategories", totalBooksInCategories);
        
        return "admin/categories";
    }
    
    @GetMapping("/categories/add")
    public String addCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category-form";
    }
    
    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category, jakarta.servlet.http.HttpServletRequest request) {
        boolean isNew = (category.getId() == null || category.getId().isEmpty());
        Category savedCategory = categoryService.save(category);
        
        // Log activity với mô tả chi tiết
        String action = isNew ? "CREATE" : "UPDATE";
        String description = isNew ? 
            "Thêm danh mục mới: \"" + category.getName() + "\"" + 
            (category.getDescription() != null && !category.getDescription().isEmpty() ? 
                " - Mô tả: " + category.getDescription() : "") : 
            "Cập nhật danh mục: \"" + category.getName() + "\"" +
            (category.getDescription() != null && !category.getDescription().isEmpty() ? 
                " - Mô tả: " + category.getDescription() : "");
        adminActivityService.logActivity(action, "CATEGORY", savedCategory.getId(), 
            category.getName(), description, request);
        
        return "redirect:/admin/categories";
    }
    
    @GetMapping("/categories/delete")
    public String deleteCategory(@RequestParam String id, jakarta.servlet.http.HttpServletRequest request) {
        Category category = categoryService.findById(id);
        String categoryName = category != null ? category.getName() : "Unknown";
        String categoryDesc = category != null && category.getDescription() != null ? 
            " - Mô tả: " + category.getDescription() : "";
        
        categoryService.deleteById(id);
        
        // Log activity
        adminActivityService.logActivity("DELETE", "CATEGORY", id, categoryName, 
            "Xóa danh mục: \"" + categoryName + "\"" + categoryDesc, request);
        
        return "redirect:/admin/categories";
    }
    
    @PostMapping("/categories/update")
    public String updateCategory(@ModelAttribute Category category, jakarta.servlet.http.HttpServletRequest request) {
        // Get existing category to track changes
        Category existingCategory = categoryService.findById(category.getId());
        
        if (existingCategory == null) {
            return "redirect:/admin/categories?error=Category+not+found";
        }
        
        // Track changes
        StringBuilder oldValues = new StringBuilder();
        StringBuilder newValues = new StringBuilder();
        StringBuilder changes = new StringBuilder();
        
        if (!existingCategory.getName().equals(category.getName())) {
            changes.append("Tên, ");
            oldValues.append("Tên: ").append(existingCategory.getName()).append("\n");
            newValues.append("Tên: ").append(category.getName()).append("\n");
        }
        
        String oldDesc = existingCategory.getDescription() != null ? existingCategory.getDescription() : "";
        String newDesc = category.getDescription() != null ? category.getDescription() : "";
        if (!oldDesc.equals(newDesc)) {
            changes.append("Mô tả, ");
            oldValues.append("Mô tả: ").append(oldDesc.isEmpty() ? "(Trống)" : oldDesc).append("\n");
            newValues.append("Mô tả: ").append(newDesc.isEmpty() ? "(Trống)" : newDesc).append("\n");
        }
        
        // Preserve createdAt
        category.setCreatedAt(existingCategory.getCreatedAt());
        
        // Save updated category
        Category savedCategory = categoryService.save(category);
        
        // Log activity with changes
        String changesStr = changes.length() > 0 ? changes.substring(0, changes.length() - 2) : "Không có thay đổi";
        String description = "Cập nhật danh mục: \"" + category.getName() + "\" - Thay đổi: " + changesStr;
        
        adminActivityService.logActivityWithChanges("UPDATE", "CATEGORY", savedCategory.getId(), 
            category.getName(), description,
            oldValues.length() > 0 ? oldValues.toString().trim() : null,
            newValues.length() > 0 ? newValues.toString().trim() : null,
            request);
        
        return "redirect:/admin/categories";
    }
    
    /**
     * Xóa tất cả sách mẫu được tạo bởi DataInitializer
     * Giữ lại các sách do admin tự tay thêm
     */
    @GetMapping("/cleanup/delete-sample-books")
    @ResponseBody
    public java.util.Map<String, Object> deleteSampleBooks() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            // Danh sách các sách mẫu từ DataInitializer
            String[] sampleBookTitles = {
                "Clean Code",
                "Design Patterns",
                "The Pragmatic Programmer",
                "Introduction to Algorithms",
                "Effective Java",
                "Head First Design Patterns"
            };
            
            int deletedCount = 0;
            List<Book> allBooks = bookService.findAll(PageRequest.of(0, 1000)).getContent();
            
            for (Book book : allBooks) {
                for (String sampleTitle : sampleBookTitles) {
                    if (book.getTitle().equals(sampleTitle)) {
                        bookService.deleteById(book.getId());
                        deletedCount++;
                        break;
                    }
                }
            }
            
            response.put("success", true);
            response.put("message", "Đã xóa " + deletedCount + " sách mẫu");
            response.put("deletedCount", deletedCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    /**
     * Xóa giỏ hàng của admin
     */
    @GetMapping("/cleanup/clear-admin-cart")
    @ResponseBody
    public java.util.Map<String, Object> clearAdminCart() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            User admin = userService.findByUsername("admin");
            if (admin != null) {
                cartService.clearCart(admin.getId());
                response.put("success", true);
                response.put("message", "Đã xóa giỏ hàng của admin");
            } else {
                response.put("success", false);
                response.put("error", "Không tìm thấy user admin");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    /**
     * Thực hiện cả hai: xóa sách mẫu và xóa giỏ hàng admin
     */
    @GetMapping("/cleanup/full-cleanup")
    @ResponseBody
    public java.util.Map<String, Object> fullCleanup() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            // Xóa sách mẫu
            String[] sampleBookTitles = {
                "Clean Code",
                "Design Patterns",
                "The Pragmatic Programmer",
                "Introduction to Algorithms",
                "Effective Java",
                "Head First Design Patterns"
            };
            
            int deletedBooksCount = 0;
            List<Book> allBooks = bookService.findAll(PageRequest.of(0, 1000)).getContent();
            
            for (Book book : allBooks) {
                for (String sampleTitle : sampleBookTitles) {
                    if (book.getTitle().equals(sampleTitle)) {
                        bookService.deleteById(book.getId());
                        deletedBooksCount++;
                        break;
                    }
                }
            }
            
            // Xóa giỏ hàng admin
            User admin = userService.findByUsername("admin");
            boolean cartCleared = false;
            if (admin != null) {
                cartService.clearCart(admin.getId());
                cartCleared = true;
            }
            
            response.put("success", true);
            response.put("message", "Đã xóa " + deletedBooksCount + " sách mẫu và " + (cartCleared ? "xóa giỏ hàng admin" : "không tìm thấy giỏ hàng admin"));
            response.put("deletedBooksCount", deletedBooksCount);
            response.put("cartCleared", cartCleared);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    /**
     * Xóa toàn bộ database (trừ users/accounts)
     * Xóa: Books, Carts, Orders, Categories
     * Giữ lại: Users (admin, user, và các users khác)
     */
    @GetMapping("/cleanup/reset-database")
    @ResponseBody
    public java.util.Map<String, Object> resetDatabase() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            // Đếm trước khi xóa
            long booksCount = bookRepository.count();
            long cartsCount = cartRepository.count();
            long ordersCount = orderRepository.count();
            long categoriesCount = categoryRepository.count();
            
            // Xóa tất cả
            bookRepository.deleteAll();
            cartRepository.deleteAll();
            orderRepository.deleteAll();
            categoryRepository.deleteAll();
            
            response.put("success", true);
            response.put("message", "Đã reset database thành công! Giữ lại tất cả users.");
            response.put("deleted", new java.util.HashMap<String, Long>() {{
                put("books", booksCount);
                put("carts", cartsCount);
                put("orders", ordersCount);
                put("categories", categoriesCount);
            }});
            response.put("note", "Users (accounts) vẫn được giữ nguyên. Bạn có thể đăng nhập bình thường.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    /**
     * Xóa tất cả giỏ hàng (của tất cả users)
     */
    @GetMapping("/cleanup/clear-all-carts")
    @ResponseBody
    public java.util.Map<String, Object> clearAllCarts() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            long count = cartRepository.count();
            cartRepository.deleteAll();
            
            response.put("success", true);
            response.put("message", "Đã xóa tất cả giỏ hàng (" + count + " giỏ)");
            response.put("deletedCount", count);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    /**
     * Xóa tất cả đơn hàng
     */
    @GetMapping("/cleanup/delete-all-orders")
    @ResponseBody
    public java.util.Map<String, Object> deleteAllOrders() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            long count = orderRepository.count();
            orderRepository.deleteAll();
            
            response.put("success", true);
            response.put("message", "Đã xóa tất cả đơn hàng (" + count + " đơn)");
            response.put("deletedCount", count);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    @GetMapping("/users/{id}")
    @ResponseBody
    public User getUserById(@PathVariable String id) {
        return userService.findById(id);
    }

    @PostMapping("/users/{id}/toggle-status")
    @ResponseBody
    public java.util.Map<String, Object> toggleUserStatus(@PathVariable String id) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            User user = userService.findById(id);
            if (user == null) {
                response.put("success", false);
                response.put("error", "Không tìm thấy người dùng");
                return response;
            }

            // Check if user has ADMIN role
            if (user.getRoles() != null && user.getRoles().contains("ADMIN")) {
                response.put("success", false);
                response.put("error", "Không thể khóa tài khoản có quyền Admin");
                return response;
            }

            boolean oldStatus = user.isEnabled();
            user.setEnabled(!user.isEnabled());
            userRepository.save(user);
            
            // Log activity với thông tin user và thay đổi trạng thái
            String action = user.isEnabled() ? "Mở khóa" : "Khóa";
            String userInfo = " - Email: " + user.getEmail() + 
                (user.getFullName() != null ? ", Họ tên: " + user.getFullName() : "");
            String description = action + " tài khoản: \"" + user.getUsername() + "\"" + userInfo;
            
            adminActivityService.logActivityWithChanges("UPDATE", "USER", id, user.getUsername(), 
                description,
                "Trạng thái: " + (oldStatus ? "Hoạt động" : "Bị khóa"),
                "Trạng thái: " + (user.isEnabled() ? "Hoạt động" : "Bị khóa"),
                null);

            response.put("success", true);
            response.put("message", user.isEnabled() ? "Đã mở khóa tài khoản" : "Đã khóa tài khoản");
            response.put("enabled", user.isEnabled());
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }

    @PostMapping("/users/{id}/delete")
    @ResponseBody
    public java.util.Map<String, Object> deleteUser(@PathVariable String id) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            User user = userService.findById(id);
            if (user == null) {
                response.put("success", false);
                response.put("error", "Không tìm thấy người dùng");
                return response;
            }

            // Check if user has ADMIN role
            if (user.getRoles() != null && user.getRoles().contains("ADMIN")) {
                response.put("success", false);
                response.put("error", "Không thể xóa tài khoản có quyền Admin");
                return response;
            }

            // Xóa giỏ hàng của user
            try {
                cartService.clearCart(user.getId());
            } catch (Exception e) {
                // Ignore if cart doesn't exist
            }
            
            String username = user.getUsername();
            String userInfo = " - Email: " + user.getEmail() + 
                (user.getFullName() != null ? ", Họ tên: " + user.getFullName() : "") +
                ", Roles: " + String.join(", ", user.getRoles());

            // Xóa user
            userRepository.deleteById(id);
            
            // Log activity
            adminActivityService.logActivity("DELETE", "USER", id, username, 
                "Xóa người dùng: \"" + username + "\"" + userInfo, null);

            response.put("success", true);
            response.put("message", "Đã xóa người dùng thành công");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
    
    // Admin Activity Tracking
    @GetMapping("/activities")
    public String viewActivities(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String adminUsername,
            @RequestParam(required = false) String timeRange,
            Model model,
            jakarta.servlet.http.HttpServletRequest request) {
        
        List<Nhom1.Demo_Nhom1.model.AdminActivity> activities;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = null;
        
        // Apply time range filter first
        if (timeRange != null && !timeRange.isEmpty()) {
            switch (timeRange) {
                case "today":
                    startDate = now.withHour(0).withMinute(0).withSecond(0);
                    break;
                case "week":
                    startDate = now.minusWeeks(1);
                    break;
                case "month":
                    startDate = now.minusMonths(1);
                    break;
                case "3months":
                    startDate = now.minusMonths(3);
                    break;
                default:
                    startDate = null;
            }
        }
        
        // Get activities based on filters
        if (startDate != null) {
            activities = adminActivityService.getActivitiesByDateRange(startDate, now);
        } else {
            activities = adminActivityService.getRecentActivities(100);
        }
        
        // Filter out VIEW actions - only show CREATE, UPDATE, DELETE
        activities = activities.stream()
            .filter(a -> !"VIEW".equals(a.getAction()))
            .collect(java.util.stream.Collectors.toList());
        
        // Apply additional filters
        if (action != null && !action.isEmpty()) {
            final String filterAction = action;
            activities = activities.stream()
                .filter(a -> filterAction.equals(a.getAction()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        if (entityType != null && !entityType.isEmpty()) {
            final String filterEntityType = entityType;
            activities = activities.stream()
                .filter(a -> filterEntityType.equals(a.getEntityType()))
                .collect(java.util.stream.Collectors.toList());
        }
        
        if (adminUsername != null && !adminUsername.isEmpty()) {
            final String filterAdmin = adminUsername;
            activities = activities.stream()
                .filter(a -> a.getAdminUsername() != null && a.getAdminUsername().contains(filterAdmin))
                .collect(java.util.stream.Collectors.toList());
        }
        
        model.addAttribute("activities", activities);
        model.addAttribute("selectedAction", action);
        model.addAttribute("selectedEntityType", entityType);
        model.addAttribute("selectedAdmin", adminUsername);
        model.addAttribute("selectedTimeRange", timeRange);
        
        // Calculate statistics
        long createCount = activities.stream().filter(a -> "CREATE".equals(a.getAction())).count();
        long updateCount = activities.stream().filter(a -> "UPDATE".equals(a.getAction())).count();
        long deleteCount = activities.stream().filter(a -> "DELETE".equals(a.getAction())).count();
        long viewCount = activities.stream().filter(a -> "VIEW".equals(a.getAction())).count();
        
        model.addAttribute("createCount", createCount);
        model.addAttribute("updateCount", updateCount);
        model.addAttribute("deleteCount", deleteCount);
        model.addAttribute("viewCount", viewCount);
        
        return "admin/activities";
    }
    
    @GetMapping("/activities/{id}")
    public String viewActivityDetail(@PathVariable String id, Model model, jakarta.servlet.http.HttpServletRequest request) {
        Nhom1.Demo_Nhom1.model.AdminActivity activity = 
            adminActivityService.getRecentActivities(1000).stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst()
                .orElse(null);
        
        if (activity == null) {
            return "redirect:/admin/activities";
        }
        
        model.addAttribute("activity", activity);
        
        // Get related activities (same entity)
        List<Nhom1.Demo_Nhom1.model.AdminActivity> relatedActivities = 
            adminActivityService.getRecentActivities(1000).stream()
                .filter(a -> activity.getEntityType() != null && 
                           activity.getEntityType().equals(a.getEntityType()) &&
                           activity.getEntityId() != null &&
                           activity.getEntityId().equals(a.getEntityId()) &&
                           !id.equals(a.getId()))
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("relatedActivities", relatedActivities);
        
        return "admin/activity-detail";
    }
}
