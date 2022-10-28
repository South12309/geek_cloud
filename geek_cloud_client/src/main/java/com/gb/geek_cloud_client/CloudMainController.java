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
    //    public TextField passwordFieldAuth;
//    public TextField loginFieldAuth;
//    public TextField passwordFieldReg;
//    public TextField loginFieldReg;
//    public Label regAnswer;
//    public Label authAnswer;
    private String currentDirectory;

    private Network<ObjectDecoderInputStream, ObjectEncoderOutputStream> network;

    private Socket socket;

    private boolean needReadMessages = true;


    public void downloadFile(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        network.getOutputStream().writeObject(new FileRequest(fileName));
    }

    public void sendToServer(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();
        Path file = Path.of(currentDirectory).resolve(fileName);
        long size = Files.size(file);
        if (size> FileUtils.FILE_PART_SIZE) {
            sendFileToServerByPart(file);
        }
        else{
            FileMessage fileMessage = new FileMessage(file);
            network.getOutputStream().writeObject(fileMessage);
        }
    }

    private void sendFileToServerByPart(Path file) {
        sendFile(file);
    }

    private void sendFile(Path file) {
        Thread sendFileThread = new Thread(() -> {

            Lock lock = new ReentrantLock();
            lock.lock();
        try (FileInputStream fileInputStream = new FileInputStream(file.toFile())) {
            String fileName = file.getFileName().toString();
            int readBuffer;
            byte[] buffer = new byte[FileUtils.FILE_PART_SIZE];
            int i =0;
            FileMessage fileMessage=null;
            while ((readBuffer = fileInputStream.read(buffer)) !=-1) {
                if (i==0) {
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
                    Files.write(Path.of(currentDirectory).resolve(fileMessage.getFileName()), fileMessage.getBytes());
                    Platform.runLater(() -> fillView(clientView, getFiles(currentDirectory)));
                } else if (message instanceof ListMessage listMessage) {
                    Platform.runLater(() -> fillView(serverView, listMessage.getFiles()));
                }
            }
        } catch (Exception e) {
            System.err.println("Server off");
            e.printStackTrace();
        }
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