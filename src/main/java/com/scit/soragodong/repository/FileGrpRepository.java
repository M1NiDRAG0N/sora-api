package com.scit.soragodong.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scit.soragodong.domain.entity.FileGrp;
import com.scit.soragodong.domain.enums.FileRefType;

public interface FileGrpRepository extends JpaRepository<FileGrp, Integer> {

    Optional<FileGrp> findByRefTypeAndRefId(FileRefType refType, Integer refId);
}
