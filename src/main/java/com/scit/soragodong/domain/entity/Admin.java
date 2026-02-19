package com.scit.soragodong.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ADMIN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "ADMIN_IDX")
	private Long adminIdx;
	
	@Column(name = "ADMIN_ID", unique = true, nullable = false, length = 50)
	private String adminId;
	
	@Column(name = "ADMIN_PASSWORD", nullable = false)
	private String adminPassword;
	
	@Column(name = "ROLE", nullable = false, length = 20)
	private String role; // "ADMIN" 또는 "OWNER"
	
	// 양방향 매핑이 필요하다면 추가 (선택 사항)
	// @OneToOne(mappedBy = "owner")
	// private Store store;
}
