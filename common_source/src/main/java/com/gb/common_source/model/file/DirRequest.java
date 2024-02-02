package com.gb.common_source.model.file;

import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.MessageType;
import lombok.Getter;

@Getter
public class DirRequest implements CloudMessage {
    private final String directory;

    public DirRequest(String directory) {
        this.directory = directory;
    }

    @Override
    public MessageType getType() {
        return MessageType.DIR_REQUEST;
    }
}
