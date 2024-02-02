package com.gb.geek_cloud_client;

import com.gb.DaemonThreadFactory;
import com.gb.model.CloudMessage;
import com.gb.model.FileMessage;
import com.gb.model.FileRequest;
import com.gb.model.ListMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class CloudMainController implements Initializable {
    public ListView<String> clientView;
    public ListView<String> serverView;
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



    private DaemonThreadFactory factory;

    public void downloadFile(ActionEvent actionEvent) throws IOException {
        String fileName = serverView.getSelectionModel().getSelectedItem();
        network.getOutputStream().writeObject(new FileRequest(fileName));
    }

    public void sendToServer(ActionEvent actionEvent) throws IOException {
        String fileName = clientView.getSelectionModel().getSelectedItem();

        String filePath = currentDirectory + "/" + fileName;
        File file = new File(filePath);
        if (file.isFile()) {
            try {
                dos.writeUTF(SEND_FILE_COMMAND);
                dos.writeUTF(fileName);
                dos.writeLong(file.length());
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] bytes = fis.readAllBytes();
                    dos.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
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
        try {
            socket = new Socket("localhost", 8189);
            network = new Network<>(
                    new ObjectDecoderInputStream(socket.getInputStream()),
                    new ObjectEncoderOutputStream(socket.getOutputStream())
            );
            factory.getThread(this::readMessages, "cloud-client-read-thread")
                    .start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        needReadMessages = true;
        factory = new DaemonThreadFactory();
        initNetwork();
        batch = new byte[BATCH_SIZE];
        setCurrentDirectory(System.getProperty("user.home"));

        setCurrentDirectoryOnServer("");
        clientView.setOnMouseClicked(event -> onMouseClickOnView(event, clientView, currentDirectory, true));
        serverView.setOnMouseClicked(event -> onMouseClickOnView(event, serverView, "", false));
    }

    private void onMouseClickOnView(MouseEvent event, ListView<String> view, String directory, boolean isClient) {
        if (event.getClickCount() == 2) {
            String selected = view.getSelectionModel().getSelectedItem();
            setDirectory(directory + "/" + selected, isClient);
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

}

