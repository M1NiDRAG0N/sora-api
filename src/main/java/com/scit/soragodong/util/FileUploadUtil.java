package com.scit.soragodong.util;

import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUploadUtil {
    
    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx"};
    
    /**
     * 파일 업로드
     */
    public static String uploadFile(MultipartFile file) throws IOException {
        validateFile(file);
        
        String filename = generateFilename(file.getOriginalFilename());
        String uploadPath = UPLOAD_DIR + "/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        Path uploadDir = Paths.get(uploadPath);
        Files.createDirectories(uploadDir);
        
        Path filePath = uploadDir.resolve(filename);
        Files.write(filePath, file.getBytes());
        
        log.info("File uploaded: {}", filePath);
        return filePath.toString();
    }
    
    /**
     * 파일 삭제
     */
    public static void deleteFile(String filepath) throws IOException {
        Path path = Paths.get(filepath);
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("File deleted: {}", filepath);
        }
    }
    
    /**
     * 파일 유효성 검사
     */
    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 10MB를 초과했습니다");
        }
        
        String extension = getFileExtension(file.getOriginalFilename());
        boolean isAllowed = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (ext.equalsIgnoreCase(extension)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다");
        }
    }
    
    /**
     * 파일 확장자 추출
     */
    private static String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 고유한 파일명 생성
     */
    private static String generateFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }
}
