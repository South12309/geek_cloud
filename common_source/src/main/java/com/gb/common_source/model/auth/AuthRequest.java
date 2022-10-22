package com.gb.common_source.model.auth;

import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.MessageType;
import com.gb.common_source.model.Request;
import lombok.Getter;

@Getter
public class AuthRequest implements CloudMessage {

    private final String login;
    private final String password;


    public AuthRequest(String login, String password) {
        this.login = login;
        this.password = password;

    }

    @Override
    public MessageType getType() {
        return MessageType.AUTH_REQUEST;
    }
}
