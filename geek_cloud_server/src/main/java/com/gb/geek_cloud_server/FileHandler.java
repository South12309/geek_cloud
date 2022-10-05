package com.gb.geek_cloud_server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileHandler implements Runnable {
    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private static final String SEND_FILE_COMMAND = "file";
    private static final String SEND_FILELIST_COMMAND = "filelist";
    private static final String GET_FILE_COMMAND = "getfile";
    private static final Integer BATCH_SIZE = 256;
    private byte[] batch;
    private static final String SERVER_DIR = "server_files";
    private String currentDirectoryServer;

    public FileHandler(Socket socket) throws IOException {
        this.socket = socket;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        batch = new byte[BATCH_SIZE];
        File file = new File(SERVER_DIR);
        if (!file.exists())
            file.mkdir();
        currentDirectoryServer = file.getCanonicalPath();
        System.out.println("Client accepted..");
    }

    private List<String> getFiles(String directory) {
        File dir = new File(directory);
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                List<String> files = new ArrayList<>(Arrays.asList(list));
                files.add(0, "..");
                return files;
            }
        }
        return List.of();
    }

    private String getRelativePath(String absolutePath, String rootPath) {
        return absolutePath.replace(rootPath, "");
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = dis.readUTF();
                if (command.equals(GET_FILE_COMMAND)) {
                    String fileName = dis.readUTF();
                    String filePath = currentDirectoryServer + "/" + fileName;
                    File file = new File(filePath);
                    if (file.isFile()) {
                        try {
                            long size = file.length();
                            dos.writeLong(size);
                            try (FileInputStream fis = new FileInputStream(file)) {
                                byte[] bytes = fis.readAllBytes();
                                dos.write(bytes);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        } catch (IOException e) {
                            System.err.println("e= " + e.getMessage());
                        }

                    }
                }
                if (command.equals(SEND_FILE_COMMAND)) {
                    String fileName = dis.readUTF();
                    long size = dis.readLong();
                    try (FileOutputStream fos = new FileOutputStream(currentDirectoryServer + "/" + fileName)) {
                        for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                            int read = dis.read(batch);
                            fos.write(batch, 0, read);
                        }
                    } catch (IOException ignored) {
                    }
                }
                if (command.equals(SEND_FILELIST_COMMAND)) {
                    String directory = dis.readUTF();
                    File dir = new File(currentDirectoryServer+directory);

                    System.out.println(dir.getCanonicalPath());
                    if (dir.isDirectory()) {
                        currentDirectoryServer = dir.getCanonicalPath();
                    }
                    List<String> files = getFiles(currentDirectoryServer);
                    int filesCount = files.size();
                    dos.writeInt(filesCount);
                    for (int i = 0; i < filesCount; i++) {
                        dos.writeUTF(files.get(i));
                    }
                //    File serverDir = new File(SERVER_DIR);
               //     String canonicalPathOfServerDir = serverDir.getCanonicalPath();
               //     String relativePath = getRelativePath(currentDirectoryServer, canonicalPathOfServerDir);
               //     dos.writeUTF(relativePath);
                } else {
                    System.out.println("Unknown command received" + command);
                }
            }
        } catch (Exception ignored) {
            System.out.println("Client disconnected...");
        }

    }
}
