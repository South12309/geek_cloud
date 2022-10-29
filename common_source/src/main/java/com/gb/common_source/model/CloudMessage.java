package com.gb.common_source.model;


import java.io.Serializable;

public interface CloudMessage extends Serializable {
    MessageType getType();
}