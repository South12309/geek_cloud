package com.gb.geek_cloud_client;

import com.gb.common_source.DaemonThreadFactory;
import com.gb.common_source.FileUtils;
import com.gb.common_source.model.*;
import com.gb.common_source.model.file.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import lombok.extern.slf4j.Slf4j;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
public class CloudMainController implements Initializable {
    public ListView<String> clientView;
    public ListView<String> serverView;
    public TextField selectedFileOnClient;
    public TextField selectedFileOnServer;
    public ProgressBar progressBar;

    private String currentDirectory;

    private Network<ObjectDecoderInputStream, ObjectEncoderOutputStream> network;

    private Socket socket;

    private static final String SEND_FILE_COMMAND = "file";
    private static final String GET_FILE_COMMAND = "getfile";
    private static final String GET_FILELIST_COMMAND = "filelist";
    private static final Integer BATCH_SIZE = 256;
    private byte[] batch;

    private String currentDirectory;
    //  private String currentDirectoryServer;




    public void downloadFile(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        network.getOutputStream().writeObject(new FileRequest(fileName));
    }

    public void sendToServer(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        Path file = Path.of(currentDirectory).resolve(fileName);
        long size = Files.size(file);
        if (size > FileUtils.FILE_PART_SIZE) {
            sendFileToServerByPart(file);
        } else {
            FileMessage fileMessage = new FileMessage(file);
            network.getOutputStream().writeObject(fileMessage);
        }
    }

    private void sendFileToServerByPart(Path file) {
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
                    progressBar.setProgress(v);
                    if (i == 0) {
                        fileMessage = new FileMessage(fileName, buffer, FileMessage.StartEndInfoEnum.START);
                    } else {
                        fileMessage = new FileMessage(fileName, buffer, FileMessage.StartEndInfoEnum.MIDDLE);
                    }
                    network.getOutputStream().writeObject(fileMessage);
                    i++;
                }
                fileMessage = new FileMessage(fileName, new byte[0], FileMessage.StartEndInfoEnum.END);
                network.getOutputStream().writeObject(fileMessage);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                progressBar.setProgress(0);
                lock.unlock();
            }
        });
        sendFileThread.setDaemon(true);
        sendFileThread.start();
    }

    private void readMessages() {
        try {
            while (needReadMessages) {
                CloudMessage message = (CloudMessage) network.getInputStream().readObject();
                if (message instanceof FileMessage fileMessage) {
                    FileUtils.writeFile(fileMessage, Path.of(currentDirectory));
                    // Files.write(Path.of(currentDirectory).resolve(fileMessage.getFileName()), fileMessage.getBytes());
                    Platform.runLater(() -> fillView(clientView, getFiles(currentDirectory)));
                } else if (message instanceof ListMessage listMessage) {
                    Platform.runLater(() -> fillView(serverView, listMessage.getFiles()));
                } else if (message instanceof Progress progress) {
                    progressBar.setProgress(progress.getProgress());

                }
            } catch (IOException e) {
                System.err.println("e= " + e.getMessage());


            }
        } catch (Exception e) {
            System.err.println("Server off");
            e.printStackTrace();
        }
    }

    public void getFromServer(ActionEvent actionEvent) {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        try {
            dos.writeUTF(GET_FILE_COMMAND);
            dos.writeUTF(fileName);
            long size = dis.readLong();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                for (int i = 0; i < (size + BATCH_SIZE - 1) / BATCH_SIZE; i++) {
                    int read = dis.read(batch);
                    fos.write(batch, 0, read);
                }
            } catch (IOException ignored) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        fillView(clientView, getFiles(currentDirectory));

    }

    private List<String> getFilesFromServer(String currentDirectoryServer) {
        List<String> files = new ArrayList<>();
        try {
            dos.writeUTF(GET_FILELIST_COMMAND);
            dos.writeUTF(currentDirectoryServer);
            int countFiles = dis.readInt();
            for (int i = 0; i < countFiles; i++) {
                files.add(dis.readUTF());
            }
            //       this.currentDirectoryServer = dis.readUTF();
            return files;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return List.of();
    }


    private void initNetwork() {
        network = Network.getInstance();
        DaemonThreadFactory factory = DaemonThreadFactory.getInstance();
        factory.getThread(this::readMessages, "cloud-client-read-thread")
                .start();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        needReadMessages = true;
        initNetwork();
        batch = new byte[BATCH_SIZE];
        setCurrentDirectory(System.getProperty("user.home"));
        fillView(clientView, getFiles(currentDirectory));
        clientView.setOnMouseClicked(event -> onMouseClickOnView(event, clientView, currentDirectory, true));
        serverView.setOnMouseClicked(event -> onMouseClickOnView(event, serverView, "", false));
    }

    private void onMouseClickOnView(MouseEvent event, ListView<String> view, String directory, boolean isClient) {
        String selected = view.getSelectionModel().getSelectedItem();
        if (event.getClickCount() == 2) {
            String delimiter = (directory.equals("")) ? "" : "/";
            setDirectory(directory + delimiter + selected, isClient);

        }
        if (event.getClickCount() == 1) {
            if (isClient) {
                selectedFileOnClient.setText(selected);
            } else {
                selectedFileOnServer.setText(selected);
            }
        }
    }

    private void setDirectory(String directory, boolean isClient) {
        if (isClient) {
            setCurrentDirectory(directory);
        } else {
            setCurrentDirectoryOnServer(directory);
        }

    }

    private void setCurrentDirectory(String directory) {
        File selectedFile = new File(directory);
        if (selectedFile.isDirectory()) {
            currentDirectory = directory;
            fillView(clientView, getFiles(currentDirectory));
        }

    }

    private void setCurrentDirectoryOnServer(String directory) {
        try {
            network.getOutputStream().writeObject(new DirRequest(directory));
            log.debug(directory + " запрошен");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void setCurrentDirectoryOnServer(String directory) {
//        currentDirectoryServer = directory;
        fillView(serverView, getFilesFromServer(directory));

    }


    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        view.getItems().addAll(data);
    }

    private List<String> getFiles(String directory) {
        // file.txt 125 b
        // dir [DIR]
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

    public void reNameOnClient(ActionEvent actionEvent) {
        Path file = Paths.get(currentDirectory, clientView.getSelectionModel().getSelectedItem());
        try {
            if (!Files.isDirectory(file)) {
                Files.move(file, file.resolveSibling(selectedFileOnClient.getText()));
                fillView(clientView, getFiles(currentDirectory));
                selectedFileOnClient.setText("");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reNameOnServer(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        network.getOutputStream().writeObject(new RenameFile(fileName, selectedFileOnServer.getText()));
        selectedFileOnServer.setText("");
    }

    public void deleteSelectedFileOnClient(ActionEvent actionEvent) {
        Path file = Paths.get(currentDirectory, clientView.getSelectionModel().getSelectedItem());
        try {
            if (!Files.isDirectory(file)) {
                Files.delete(file);
                fillView(clientView, getFiles(currentDirectory));
                selectedFileOnClient.setText("");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void deleteSelectedFileOnServer(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        network.getOutputStream().writeObject(new DeleteFile(fileName));
        selectedFileOnServer.setText("");
    }
}

