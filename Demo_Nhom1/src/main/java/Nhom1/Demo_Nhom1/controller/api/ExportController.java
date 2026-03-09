package Nhom1.Demo_Nhom1.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Nhom1.Demo_Nhom1.model.Book;
import Nhom1.Demo_Nhom1.model.Order;
import Nhom1.Demo_Nhom1.service.BookService;
import Nhom1.Demo_Nhom1.service.ExportService;
import Nhom1.Demo_Nhom1.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/api/export")
@PreAuthorize("hasRole('ADMIN')")
public class ExportController {
    
    @Autowired
    private ExportService exportService;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping("/books/excel")
    public ResponseEntity<byte[]> exportBooksToExcel() {
        try {
            List<Book> books = bookService.findAll(PageRequest.of(0, 1000)).getContent();
            byte[] data = exportService.exportBooksToExcel(books);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "books.xlsx");
            
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/orders/pdf")
    public ResponseEntity<byte[]> exportOrdersToPDF() {
        try {
            List<Order> orders = orderService.findAll(PageRequest.of(0, 1000)).getContent();
            byte[] data = exportService.exportOrdersToPDF(orders);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "orders.pdf");
            
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
