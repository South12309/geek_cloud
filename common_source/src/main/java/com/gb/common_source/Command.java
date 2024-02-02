package com.gb.common_source;

public enum Command {

    SEND_FILE_COMMAND("file"),
    GET_FILE_COMMAND("get_file"),
    GET_FILES_LIST_COMMAND("list");

    private final String simpleName;

    Command(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getSimpleName() {
        return simpleName;
    }
}