package com.gb.common_source.model.file;

import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.MessageType;
import lombok.Getter;

@Getter
public class RenameFile implements CloudMessage {

    private final String fileName;
    private final String newFileName;

    public RenameFile(String fileName, String newFileName) {
        this.fileName = fileName;
        this.newFileName = newFileName;
    }

    @Override
    public MessageType getType() {
        return MessageType.FILE;
    }
}
