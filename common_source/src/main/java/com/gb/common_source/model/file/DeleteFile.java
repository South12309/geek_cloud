package com.gb.common_source.model.file;

import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.MessageType;
import lombok.Getter;

@Getter
public class DeleteFile implements CloudMessage {
    private final String fileName;

    public DeleteFile(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public MessageType getType() {
        return MessageType.FILE;
    }
}
