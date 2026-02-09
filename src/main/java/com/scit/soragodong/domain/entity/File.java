package com.scit.soragodong.domain.entity;

import com.scit.soragodong.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "FILE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class File extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FILE_IDX")
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
    
    @Builder.Default
    @Column(name = "IS_USE", nullable = false)
    private Boolean isUse = true;

}
