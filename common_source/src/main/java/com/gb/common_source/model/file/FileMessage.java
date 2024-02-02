package com.gb.common_source.model.file;

import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.MessageType;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
public class FileMessage implements CloudMessage {

    private final String fileName;
    private final long size;
    private final byte[] bytes;
    private final boolean isMultiPart;
    private StartEndInfoEnum startEndInfo;

    public static enum StartEndInfoEnum {
        START,
        MIDDLE,
        END
    }


    public FileMessage(Path file) throws IOException {
        fileName = file.getFileName().toString();
        bytes = Files.readAllBytes(file);
        size = bytes.length;
        this.isMultiPart = false;
    }
    public FileMessage(String fileName, byte[] bytes, StartEndInfoEnum startEndInfo) throws IOException {
        this.fileName = fileName;
        this.bytes = bytes;
        size = bytes.length;
        isMultiPart = true;
        this.startEndInfo = startEndInfo;
    }

    @Override
    public MessageType getType() {
        return MessageType.FILE;
    }
}