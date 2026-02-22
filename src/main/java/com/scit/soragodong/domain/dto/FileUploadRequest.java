package com.scit.soragodong.domain.dto;

import org.hibernate.id.IntegralDataTypeHolder;

import com.scit.soragodong.domain.enums.FileRefType;

public record FileUploadRequest(
                FileRefType refType,
                IntegralDataTypeHolder refId) {
}
