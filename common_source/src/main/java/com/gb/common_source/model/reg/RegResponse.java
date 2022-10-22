package com.gb.common_source.model.reg;

import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.MessageType;
import lombok.Getter;

@Getter
public class RegResponse implements CloudMessage {
    private final RegResponseEnum regResponseEnum;

    public RegResponse(RegResponseEnum regResponseEnum) {
        this.regResponseEnum = regResponseEnum;
    }

    @Override
    public MessageType getType() {
        return MessageType.REG_RESPONSE;
    }
}
