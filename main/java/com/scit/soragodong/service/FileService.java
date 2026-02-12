package com.scit.soragodong.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.scit.soragodong.domain.dto.FileRes;
import com.scit.soragodong.domain.entity.File;
import com.scit.soragodong.domain.entity.FileGrp;
import com.scit.soragodong.domain.enums.FileRefType;
import com.scit.soragodong.exception.CustomException;
import com.scit.soragodong.exception.ErrorCode;
import com.scit.soragodong.repository.FileGrpRepository;
import com.scit.soragodong.repository.FileRepository;
import com.scit.soragodong.util.FileUploadUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FileService {

    private final FileGrpRepository fileGroupRepository;
    private final FileRepository fileRepository;
    private final FileUploadUtil fileUploadUtil;

    public List<FileRes> upload(FileRefType refType, Integer refId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        FileGrp group = fileGroupRepository
                .findByRefTypeAndRefId(refType, refId)
                .orElseGet(() -> fileGroupRepository.save(
                        FileGrp.builder()
                                .refType(refType)
                                .refId(refId)
                                .build()
                ));

        List<FileRes> responses = new ArrayList<>();
        int order = 0;

        for (MultipartFile file : files) {
            try {
                // 파일 업로드 (실제 저장)
                String relativePath = fileUploadUtil.uploadFile(file);
                
                // DB에 저장
                File saved = fileRepository.save(
                        File.builder()
                                .fileGroup(group)
                                .fileName(getFilenameWithoutExt(file.getOriginalFilename()))
                                .originalName(file.getOriginalFilename())
                                .fileExt(getFileExtension(file.getOriginalFilename()))
                                .fileSize((int) file.getSize())
                                .filePath(relativePath)
                                .fileOrder(order++)
                                .isUse(true)
                                .build()
                );

                responses.add(new FileRes(
                        saved.getFileIdx(),
                        saved.getOriginalName(),
                        relativePath,
                        saved.getFileOrder()
                ));
            } catch (IOException e) {
                throw new CustomException(ErrorCode.INTERNAL_ERROR);
            }
        }

        return responses;
    }

    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String getFilenameWithoutExt(String filename) {
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
