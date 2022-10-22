package com.gb.common_source.model;

public class AuthRequest implements CloudMessage{

    @Override
    public MessageType getType() {
        return MessageType.AUTH_REQUEST;
    }
}
