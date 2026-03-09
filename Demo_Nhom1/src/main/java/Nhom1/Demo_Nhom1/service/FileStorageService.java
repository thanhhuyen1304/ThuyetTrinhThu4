package Nhom1.Demo_Nhom1.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    
    private final Path bookStorageLocation;
    private final Path avatarStorageLocation;
    
    public FileStorageService() {
        this.bookStorageLocation = Paths.get("uploads/books").toAbsolutePath().normalize();
        this.avatarStorageLocation = Paths.get("uploads/avatars").toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.bookStorageLocation);
            Files.createDirectories(this.avatarStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create upload directory!", ex);
        }
    }
    
    public String storeFile(MultipartFile file) {
        return storeFile(file, "books");
    }
    
    public String storeFile(MultipartFile file, String type) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }
            
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + fileExtension;
            
            Path storageLocation = "avatars".equals(type) ? this.avatarStorageLocation : this.bookStorageLocation;
            Path targetLocation = storageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return "/uploads/" + type + "/" + newFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }
    
    public void deleteFile(String filename) {
        deleteFile(filename, "books");
    }
    
    public void deleteFile(String filename, String type) {
        try {
            Path storageLocation = "avatars".equals(type) ? this.avatarStorageLocation : this.bookStorageLocation;
            Path filePath = storageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            // Log error
        }
    }
}
