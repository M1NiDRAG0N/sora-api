package com.scit.soragodong.domain.entity;

import java.time.LocalDateTime;


import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "FILE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer fileIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_GRP_IDX")
    private FileGrp fileGroup;

    private String fileName;
    private String originalName;
    private String fileExt;
    private Integer fileSize;
    private String filePath;
    private Integer fileOrder;
    private Boolean isUse;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 파일 업로드 생성자
     */
    public File(FileGrp fileGroup, String fileName, String originalName, String fileExt, 
                int fileSize, String filePath, int fileOrder) {
        this.fileGroup = fileGroup;
        this.fileName = fileName;
        this.originalName = originalName;
        this.fileExt = fileExt;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.fileOrder = fileOrder;
        this.isUse = true;
    }

}
