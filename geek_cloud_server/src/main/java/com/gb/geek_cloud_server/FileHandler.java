package com.gb.geek_cloud_server;

import com.gb.common_source.model.*;
import com.gb.common_source.model.auth.AuthRequest;
import com.gb.common_source.model.file.*;
import com.gb.common_source.model.reg.RegRequest;
import com.gb.common_source.model.reg.RegResponse;
import com.gb.common_source.model.reg.RegResponseEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path rootDir =Path.of("server_files");;
    private Path dirUserName;

    private AuthService authService;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      //  serverDirUserName = rootDir+;
        dirUserName=rootDir;
      // ctx.writeAndFlush(new ListMessage(dirUserName));
        authService = new SQLAuthService();
        authService.run();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws IOException {
        authService.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
        log.debug("Received: {}", cloudMessage.getType());
        if (cloudMessage instanceof FileMessage fileMessage) {
            Files.write(dirUserName.resolve(fileMessage.getFileName()), fileMessage.getBytes());
            ctx.writeAndFlush(new ListMessage(dirUserName));
        } else if (cloudMessage instanceof FileRequest fileRequest) {
            Path file = dirUserName.resolve(fileRequest.getFileName());
            if (!Files.isDirectory(file)) {
                ctx.writeAndFlush(new FileMessage(file));
            }
        } else if (cloudMessage instanceof DirRequest dirRequest) {
            Path dir = dirUserName.resolve(dirRequest.getDirectory());
            if (Files.isDirectory(dir)) {
                dirUserName =dir;
                ctx.writeAndFlush(new ListMessage(dir));
            }
        } else if (cloudMessage instanceof RenameFile renameFile) {
            Path file = dirUserName.resolve(renameFile.getFileName());
            if (!Files.isDirectory(file)) {
                Files.move(file, file.resolveSibling(renameFile.getNewFileName()));
                ctx.writeAndFlush(new ListMessage(dirUserName));
            }

        } else if (cloudMessage instanceof DeleteFile deleteFile) {
            Path file = dirUserName.resolve(deleteFile.getFileName());
            if (!Files.isDirectory(file)) {
                Files.delete(file);
                ctx.writeAndFlush(new ListMessage(dirUserName));
            }

        } else if (cloudMessage instanceof AuthRequest authRequest) {
            ctx.writeAndFlush(authService.authorization(authRequest));
            setDirUserName(authService.getDirNameLogin());
            ctx.writeAndFlush(new ListMessage(dirUserName));

        } else if (cloudMessage instanceof RegRequest regRequest) {
            ctx.writeAndFlush(authService.registration(regRequest));
         //  ctx.writeAndFlush(new RegResponse(RegResponseEnum.REG_OK));

        }

    }

    private void setDirUserName(String dirNameLogin) throws IOException {
        Path tempDir = rootDir.resolve(dirNameLogin);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        dirUserName = tempDir;
    }
}