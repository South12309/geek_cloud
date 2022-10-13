package com.gb.common_source.model;

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
