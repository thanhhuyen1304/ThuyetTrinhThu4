package Nhom1.Demo_Nhom1.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import Nhom1.Demo_Nhom1.model.Book;
import Nhom1.Demo_Nhom1.model.Order;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExportService {
    
    public byte[] exportBooksToExcel(List<Book> books) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Books");
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Title", "Author", "Category", "Price", "Stock"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            int rowNum = 1;
            for (Book book : books) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(book.getId());
                row.createCell(1).setCellValue(book.getTitle());
                row.createCell(2).setCellValue(book.getAuthor());
                row.createCell(3).setCellValue(book.getCategory());
                row.createCell(4).setCellValue(book.getPrice());
                row.createCell(5).setCellValue(book.getStockQuantity());
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
    
    public byte[] exportOrdersToPDF(List<Order> orders) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        PdfWriter.getInstance(document, out);
        document.open();
        
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
        Paragraph title = new Paragraph("Orders Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));
        
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        
        addTableHeader(table, new String[]{"Order ID", "Customer", "Total", "Status", "Date"});
        
        for (Order order : orders) {
            table.addCell(order.getId());
            table.addCell(order.getUsername());
            table.addCell(String.format("%.2f", order.getTotalAmount()));
            table.addCell(order.getStatus());
            table.addCell(order.getCreatedAt().toString());
        }
        
        document.add(table);
        document.close();
        
        return out.toByteArray();
    }
    
    private void addTableHeader(PdfPTable table, String[] headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }
}
