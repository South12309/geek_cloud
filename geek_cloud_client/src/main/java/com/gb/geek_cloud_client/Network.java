package com.gb.geek_cloud_client;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

public class Network<I, O> {

    private static Network network;
    private static Socket socket;

    public Network(I inputStream, O outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    private final I inputStream;

    private final O outputStream;

    public static Network getInstance() {
        if (network==null) {
            try {
                socket = new Socket("localhost", 8189);

            network = new Network<>(
                    new ObjectDecoderInputStream(socket.getInputStream()),
                    new ObjectEncoderOutputStream(socket.getOutputStream())
            );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return network;
    }

    public I getInputStream() {
        return inputStream;
    }

    public O getOutputStream() {
        return outputStream;
    }

}