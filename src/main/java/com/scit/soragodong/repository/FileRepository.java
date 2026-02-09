package com.scit.soragodong.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scit.soragodong.domain.entity.File;
import com.scit.soragodong.domain.entity.FileGrp;

public interface FileRepository extends JpaRepository<File, Integer> {

    List<FileGrp> findByFileGroupAndIsUseTrueOrderByFileOrder(FileGrp group);
    
    /**
     * 파일 그룹에서 첫 번째 파일 조회 (썸네일용)
     */
    Optional<File> findFirstByFileGroupAndIsUseTrueOrderByFileOrder(FileGrp fileGroup);
}
