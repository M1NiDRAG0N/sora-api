package com.scit.soragodong.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.scit.soragodong.domain.entity.File;
import com.scit.soragodong.domain.entity.FileGrp;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.FileRepository;
import com.scit.soragodong.repository.FileGrpRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/img")
@RequiredArgsConstructor
public class FileController {

    @Value("${spring.file.upload.path:/upload}")
    private String basePath;

    private final FileRepository fileRepository;
    private final FileGrpRepository fileGrpRepository;

    /**
     * 파일 ID로 이미지 조회
     * 예: /img/1
     */
    @GetMapping("/{fileIdx}")
    public ResponseEntity<byte[]> getFileByIdx(@PathVariable("fileIdx") int fileIdx) {
        try {
            Optional<File> fileOpt = fileRepository.findById(fileIdx);
            if (fileOpt.isEmpty()) {
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            }

            File file = fileOpt.get();
            return buildImageResponse(file);
        } catch (IOException e) {
            log.error("파일 조회 실패: {}", fileIdx, e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 파일 그룹 ID로 첫 번째 이미지 조회 (썸네일용)
     * 예: /img/group/1
     */
    @GetMapping("/group/{fileGrpIdx}")
    public ResponseEntity<byte[]> getThumbnailByGroupIdx(@PathVariable("fileGrpIdx") int fileGrpIdx) {
        try {
            Optional<FileGrp> groupOpt = fileGrpRepository.findById(fileGrpIdx);
            if (groupOpt.isEmpty()) {
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            }

            FileGrp fileGrp = groupOpt.get();
            Optional<File> fileOpt = fileRepository.findFirstByFileGroupAndIsUseTrueOrderByFileOrder(fileGrp);
            if (fileOpt.isEmpty()) {
                throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
            }

            File file = fileOpt.get();
            return buildImageResponse(file);
        } catch (IOException e) {
            log.error("썸네일 조회 실패: {}", fileGrpIdx, e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 이미지 응답 생성
     */
    private ResponseEntity<byte[]> buildImageResponse(File file) throws IOException {
        // 파일 경로 생성
        Path filePath = Paths.get(basePath + file.getFilePath());

        // 파일이 존재하는지 확인
        if (!Files.exists(filePath)) {
            log.warn("파일을 찾을 수 없습니다: {}", filePath);
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 파일 읽기
        byte[] fileContent = Files.readAllBytes(filePath);

        // Content-Type 결정
        MediaType mediaType = getMediaType(file.getFileExt());

        // 응답 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(fileContent.length);
        headers.setCacheControl("max-age=86400"); // 24시간 캐시

        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }

    /**
     * 파일 확장자에 따라 MediaType 결정
     */
    private MediaType getMediaType(String fileExt) {
        if (fileExt == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        String ext = fileExt.toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.valueOf("image/webp");
            case "svg" -> MediaType.valueOf("image/svg+xml");
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
