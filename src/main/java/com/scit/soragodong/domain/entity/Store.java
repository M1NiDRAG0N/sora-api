package com.scit.soragodong.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "STORE")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STORE_IDX", nullable = false)
    private Integer storeIdx;

    @Column(name = "STORE_NAME", nullable = false, length = 100)
    private String storeName;

    @Column(name = "STORE_ADDRESS", nullable = false, length = 200)
    private String storeAddress;

    @Column(name = "STORE_OPEN_TIME", nullable = false)
    private LocalTime storeOpenTime;

    @Column(name = "STORE_CLOSE_TIME", nullable = false)
    private LocalTime storeCloseTime;

    @Column(name = "EVENT_START_TIME")
    private LocalTime eventStartTime;

    @Column(name = "EVENT_END_TIME")
    private LocalTime eventEndTime;

    @Column(name = "EVENT_STATE", length = 50)
    private String eventState;

    @Column(name = "EVENT_NOTE", length = 100)
    private String eventNote;

    @Column(name = "STORE_PICTURE_IDX", length = 100)
    private String storePictureIdx;

    @Column(name = "IS_USE", nullable = false)
    @Builder.Default
    private Byte isUse = 1;

    @CreationTimestamp
    @Column(name = "CREATE_AT", updatable = false)
    private LocalDateTime createAt;

    @Column(name = "STORE_LAT")
    private Double storeLat;

    @Column(name = "STORE_LNG")
    private Double storeLng;
	
	
}
