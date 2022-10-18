package com.gb.geek_cloud_server.netty.serial;

import com.gb.common_source.model.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path serverDir;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        serverDir = Path.of("server_files");
        ctx.writeAndFlush(new ListMessage(serverDir));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        log.debug("Received: {}", cloudMessage.getType());
        if (cloudMessage instanceof FileMessage fileMessage) {
            Files.write(serverDir.resolve(fileMessage.getFileName()), fileMessage.getBytes());
            ctx.writeAndFlush(new ListMessage(serverDir));
        } else if (cloudMessage instanceof FileRequest fileRequest) {
            Path file = serverDir.resolve(fileRequest.getFileName());
            if (!Files.isDirectory(file)) {
                ctx.writeAndFlush(new FileMessage(file));
            }
        } else if (cloudMessage instanceof DirRequest dirRequest) {
            Path dir = serverDir.resolve(dirRequest.getDirectory());
            if (Files.isDirectory(dir)) {
                serverDir=dir;
                ctx.writeAndFlush(new ListMessage(dir));
            }
        } else if (cloudMessage instanceof RenameFile renameFile) {
            Path file = serverDir.resolve(renameFile.getFileName());
            if (!Files.isDirectory(file)) {
                Files.move(file, file.resolveSibling(renameFile.getNewFileName()));
                ctx.writeAndFlush(new ListMessage(serverDir));
            }

        } else if (cloudMessage instanceof DeleteFile deleteFile) {
            Path file = serverDir.resolve(deleteFile.getFileName());
            if (!Files.isDirectory(file)) {
                Files.delete(file);
                ctx.writeAndFlush(new ListMessage(serverDir));
            }

        }

    }
}