package com.scit.soragodong.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileRefType {

    USER_PROFILE("USERS"),
    BOARD("BOARD"),
    STORE("STORE"),
    PRODUCT("PRODUCT"),
    USED("USED");

    private final String code;
}

