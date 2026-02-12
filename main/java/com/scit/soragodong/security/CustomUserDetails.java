package com.scit.soragodong.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.scit.soragodong.domain.entity.Users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CustomUserDetails implements UserDetails {

    private Integer userIdx;
    private String userEmail;
    private String password;
    private String userName;
    private String userNickname;
    private String userAddress;
    private String userRole;
    private String userBadge;
    private Integer profileIdx;
    private Integer mannerScore;
    private Integer monthlyBudget;
    private Double userLat;
    private Double userLng;
    private Boolean isUse;

    public static CustomUserDetails of(Users user) {
        return new CustomUserDetails(
                user.getUserIdx(),
                user.getUserEmail(),
                user.getPassword(),
                user.getUserName(),
                user.getUserNickname(),
                user.getUserAddress(),
                user.getUserRole().name(),
                user.getUserBadge(),
                user.getProfileIdx(),
                user.getMannerScore(),
                user.getMonthlyBudget(),
                user.getUserLat(),
                user.getUserLng(),
                user.getIsUse());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userEmail;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isUse;
    }
}
