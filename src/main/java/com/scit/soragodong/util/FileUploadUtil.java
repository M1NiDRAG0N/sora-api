package com.scit.soragodong.util;

import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileUploadUtil {
    
    @Value("${spring.file.upload.path:/upload}")
    private String basePath;
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx"};
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    
    /**
     * 파일 업로드
     */
    public String uploadFile(MultipartFile file) throws IOException {
        validateFile(file);
        
        String filename = generateFilename(file.getOriginalFilename());
        String yearMonth = LocalDateTime.now().format(FORMATTER);
        String uploadPath = basePath + "/" + yearMonth;
        
        Path uploadDir = Paths.get(uploadPath);
        Files.createDirectories(uploadDir);
        
        Path filePath = uploadDir.resolve(filename);
        Files.write(filePath, file.getBytes());
        
        log.info("File uploaded: {}", filePath);
        
        // DB에 저장할 상대 경로 반환
        return "/" + yearMonth + "/" + filename;
    }
    
    /**
     * 파일 삭제
     */
    public void deleteFile(String relativeFilePath) throws IOException {
        Path path = Paths.get(basePath + relativeFilePath);
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("File deleted: {}", path);
        }
    }
    
    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
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
    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 고유한 파일명 생성
     */
    private String generateFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }
}
