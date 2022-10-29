package com.gb.common_source.model.auth;

import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.MessageType;
import lombok.Getter;

@Getter
public class AuthResponse implements CloudMessage {

    private final AuthResponseEnum authResponseEnum;

    public AuthResponse(AuthResponseEnum authResponseEnum) {
        this.authResponseEnum = authResponseEnum;
    }

    @Override
    public MessageType getType() {
        return MessageType.AUTH_RESPONSE;
    }

}
