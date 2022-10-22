package com.gb.common_source.model.reg;

import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.MessageType;
import lombok.Getter;

@Getter
public class RegRequest implements CloudMessage {
    private final String login;
    private final String password;

    public RegRequest(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    public MessageType getType() {
        return MessageType.REG_REQUEST;
    }
}
