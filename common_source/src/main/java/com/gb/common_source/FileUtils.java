package com.gb.common_source;

import com.gb.common_source.model.file.FileMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.APPEND;
@Slf4j
public class FileUtils {

    public static int FILE_PART_SIZE=5000;

    public static void writeFile(FileMessage fileMessage, Path dirUserName) throws IOException {
        Path file_temp = dirUserName.resolve(fileMessage.getFileName()+"_temp");
        Path file = dirUserName.resolve(fileMessage.getFileName());
        if (fileMessage.isMultiPart()) {
            if (fileMessage.getStartEndInfo()== FileMessage.StartEndInfoEnum.START) {
                Files.createFile(file_temp);
                Files.write(file_temp, fileMessage.getBytes(), APPEND);
                log.info(fileMessage.getFileName() + " start");
            }
            if (fileMessage.getStartEndInfo()== FileMessage.StartEndInfoEnum.MIDDLE) {
                Files.write(file_temp, fileMessage.getBytes(), APPEND);
                log.info(fileMessage.getFileName() + " middle");
            }
            if (fileMessage.getStartEndInfo()== FileMessage.StartEndInfoEnum.END) {
                Files.move(file_temp, file);
                log.info(fileMessage.getFileName() + " end");
            }
        } else {
            Files.write(dirUserName.resolve(fileMessage.getFileName()), fileMessage.getBytes());
        }
    }


//    public static void readFileFromStream(DataInputStream dis, String dstDirectory) throws IOException {
//        byte[] batch = new byte[BATCH_SIZE];
//        String fileName = dis.readUTF();
//        long size = dis.readLong();
//        System.out.println("File name: " + fileName + ", file size: " + size);
//        try (FileOutputStream fos = new FileOutputStream(dstDirectory + "/" + fileName)) {
//            for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
//                int read = dis.read(batch);
//                fos.write(batch, 0, read);
//            }
//        } catch (Exception ignored) {
//        }
//    }

}