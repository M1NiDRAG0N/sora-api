package com.scit.soragodong.domain.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import com.scit.soragodong.domain.enums.FileRefType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "FILE_GRP")
@Getter
@NoArgsConstructor
public class FileGrp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FILE_GRP_IDX")
    private Integer fileGrpIdx;

    @Enumerated(EnumType.STRING)
    @Column(name = "REF_TYPE", nullable = false)
    private FileRefType refType;

    @Column(name = "REF_IDX", nullable = false)
    private Integer refId;

    @CreatedDate
    @Column(name = "CREATED_DT")
    private LocalDateTime createdAt = LocalDateTime.now();

    public FileGrp(FileRefType refType, Integer refId) {
        this.refType = refType;
        this.refId = refId;
    }
}
