package com.gb.geek_cloud_client;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class CloudMainController implements Initializable {
    public ListView<String> clientView;
    public ListView<String> serverView;
    private DataInputStream dis;
    private DataOutputStream dos;
    private Socket socket;
    private static final String SEND_FILE_COMMAND = "file";
    private static final String SEND_FILELIST_COMMAND = "filelist";

    private String currentDirectory;
    private String currentDirectoryServer;

    public void sendToServer(ActionEvent actionEvent) {
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

        }
    }

    private List<String> getFilesFromServer(String currentDirectoryServer) {
        List<String> files = new ArrayList<>();
        try {
            dos.writeUTF(SEND_FILELIST_COMMAND);
            dos.writeUTF(currentDirectoryServer);
            int countFiles = dis.readInt();
            for (int i = 0; i < countFiles; i++) {
                files.add(dis.readUTF());
            }
            this.currentDirectoryServer = dis.readUTF();
            return files;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return List.of();
    }


    private void initNetwork() {
        try {
            socket = new Socket("localhost", 8189);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initNetwork();
        setCurrentDirectory(System.getProperty("user.home"));
        setCurrentDirectoryServer("");
        clientView.setOnMouseClicked(event -> onMouseClickOnView(event, clientView, currentDirectory, true));
        serverView.setOnMouseClicked(event -> onMouseClickOnView(event, serverView, currentDirectoryServer, false));
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
            setCurrentDirectoryServer(directory);
        }
    }

    private void setCurrentDirectory(String directory) {
        File selectedFile = new File(directory);
        if (selectedFile.isDirectory()) {
            currentDirectory = directory;
            fillView(clientView, getFiles(currentDirectory));
        }
    }

    private void setCurrentDirectoryServer(String directory) {
        currentDirectoryServer = directory;
        fillView(serverView, getFilesFromServer(currentDirectoryServer));

    }


    private void fillView(ListView<String> view, List<String> data) {
        view.getItems().clear();
        view.getItems().addAll(data);
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
}
