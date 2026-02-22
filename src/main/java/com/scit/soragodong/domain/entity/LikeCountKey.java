package com.scit.soragodong.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeCountKey implements Serializable {
    @Column(name = "USER_IDX")
    private Integer userIdx; // 객체가 아니라 그냥 숫자 ID

    @Column(name = "BOARD_IDX")
    private Integer boardIdx; // 객체가 아니라 그냥 숫자 ID

}