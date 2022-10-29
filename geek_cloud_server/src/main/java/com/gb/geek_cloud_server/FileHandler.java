package com.gb.geek_cloud_server;

import com.gb.common_source.FileUtils;
import com.gb.common_source.model.*;
import com.gb.common_source.model.auth.AuthRequest;
import com.gb.common_source.model.file.*;
import com.gb.common_source.model.reg.RegRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class FileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path rootDir = Path.of("server_files");
    ;
    private Path dirUserName;

    private AuthService authService;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //  serverDirUserName = rootDir+;
        dirUserName = rootDir;
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
            FileUtils.writeFile(fileMessage, dirUserName);
            ctx.writeAndFlush(new ListMessage(dirUserName));
        } else if (cloudMessage instanceof FileRequest fileRequest) {
            String fileName = fileRequest.getFileName();
            Path file = dirUserName.resolve(fileName);
            long size = Files.size(file);
            if (size > FileUtils.FILE_PART_SIZE) {
                sendFileToClientByPart(file, ctx);
            } else {
                FileMessage fileMessage = new FileMessage(file);
                ctx.writeAndFlush(fileMessage);
            }

        } else if (cloudMessage instanceof DirRequest dirRequest) {
            Path dir = dirUserName.resolve(dirRequest.getDirectory());
            if (Files.isDirectory(dir)) {
                dirUserName = dir;
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

    private void sendFileToClientByPart(Path file, ChannelHandlerContext ctx) {
        Thread sendFileThread = new Thread(() -> {
            Lock lock = new ReentrantLock();
            lock.lock();
            try (FileInputStream fileInputStream = new FileInputStream(file.toFile())) {
                float sizeFile = Files.size(file);
                float partCount = sizeFile / FileUtils.FILE_PART_SIZE;
                float progressPart = partCount/100;


                String fileName = file.getFileName().toString();
                int readBuffer;
                byte[] buffer = new byte[FileUtils.FILE_PART_SIZE];
                int i = 0;
                FileMessage fileMessage = null;
                while ((readBuffer = fileInputStream.read(buffer)) != -1) {
                    float v = i * progressPart/100 ;
                    ctx.writeAndFlush(new Progress(v));
                    if (i == 0) {
                        fileMessage = new FileMessage(fileName, buffer, FileMessage.StartEndInfoEnum.START);
                    } else {
                        fileMessage = new FileMessage(fileName, buffer, FileMessage.StartEndInfoEnum.MIDDLE);
                    }
                    ctx.writeAndFlush(fileMessage);
                    i++;
                }
                fileMessage = new FileMessage(fileName, new byte[0], FileMessage.StartEndInfoEnum.END);
                ctx.writeAndFlush(fileMessage);
                ctx.writeAndFlush(new Progress(0));


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        });
        sendFileThread.setDaemon(true);
        sendFileThread.start();
    }

    private void setDirUserName(String dirNameLogin) throws IOException {
        Path tempDir = rootDir.resolve(dirNameLogin);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        dirUserName = tempDir;
    }
}