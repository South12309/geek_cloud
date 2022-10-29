package com.gb.common_source.model.file;

import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.MessageType;
import lombok.Getter;

@Getter
public class Progress implements CloudMessage {
    private final float progress;

    public Progress(float progress) {
        this.progress = progress;
    }

    @Override
    public MessageType getType() {
        return MessageType.PROGRESS;
    }
}
