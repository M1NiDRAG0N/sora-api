package com.scit.soragodong.domain.entity;

import com.scit.soragodong.common.BaseEntity;
import com.scit.soragodong.domain.enums.FileRefType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "FILE_GRP")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FileGrp extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "REF_TYPE", nullable = false)
    private FileRefType refType;

    @Column(name = "REF_IDX", nullable = false)
    private Integer refId;

}
