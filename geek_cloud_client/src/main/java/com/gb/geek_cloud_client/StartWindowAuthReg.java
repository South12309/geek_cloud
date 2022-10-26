package com.gb.geek_cloud_client;

import com.gb.common_source.DaemonThreadFactory;
import com.gb.common_source.model.CloudMessage;
import com.gb.common_source.model.auth.AuthRequest;
import com.gb.common_source.model.auth.AuthResponse;
import com.gb.common_source.model.auth.AuthResponseEnum;
import com.gb.common_source.model.reg.RegRequest;
import com.gb.common_source.model.reg.RegResponse;
import com.gb.common_source.model.reg.RegResponseEnum;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class StartWindowAuthReg implements Initializable {
    public TextField loginField;
    public TextField passwordField;
    public Button submitButton;
    public Button regButton;
    public Button backButton;
    public Label authRegLabel;
    private ActionEvent thisEvent;
//    private Stage stage;
//    private Scene scene;
//    private Parent root;
  //  private String test = "null";
 //   private DaemonThreadFactory factory;
    private Thread thread;
    private Network<ObjectDecoderInputStream, ObjectEncoderOutputStream> network;

    private void switchFormToReg() throws IOException {
        regButton.setVisible(false);
        submitButton.setText("Зарегистрироваться");
        authRegLabel.setText("Регистрация");
        backButton.setVisible(true);
        submitButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    onRegistration(event);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    private void switchFormToAuth() throws IOException {
        regButton.setVisible(true);
        submitButton.setText("Войти");
        authRegLabel.setText("Авторизация");
        backButton.setVisible(false);
        submitButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    onAuthorization(event);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    public void onAuthorization(ActionEvent actionEvent) throws IOException {
        thisEvent = actionEvent;
        network.getOutputStream().writeObject(new AuthRequest(loginField.getText(), passwordField.getText()));
    }

    public void onRegistration(ActionEvent actionEvent) throws IOException {
        thisEvent = actionEvent;
        network.getOutputStream().writeObject(new RegRequest(loginField.getText(), passwordField.getText()));
    }

    public void onSwitchToRegWindow(ActionEvent actionEvent) throws IOException {
        thisEvent = actionEvent;
        switchFormToReg();
    }

    public void onBack(ActionEvent actionEvent) throws IOException {
        thisEvent = actionEvent;
        switchFormToAuth();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initNetwork();
    }

    private void initNetwork() {
        network = Network.getInstance();
        DaemonThreadFactory factory = DaemonThreadFactory.getInstance();
        thread = factory.getThread(this::auth, "cloud-client-auth-thread");
        thread.start();
    }

    private void auth() {
        while (true) {
            try {
                log.debug("auth");
                CloudMessage cloudMessage = (CloudMessage) network.getInputStream().readObject();
                if (cloudMessage instanceof AuthResponse authResponse) {
                    if (authResponse.getAuthResponseEnum().equals(AuthResponseEnum.AUTH_OK)) {
                        Platform.runLater(() -> switchScene("geek_cloud_client.fxml"));
                        break;
                    } else if (authResponse.getAuthResponseEnum().equals(AuthResponseEnum.AUTH_WRONG)) {
                        Platform.runLater(() -> showError("Ошибка авторизации"));
                    }
                } else if (cloudMessage instanceof RegResponse regResponse) {
                    if (regResponse.getRegResponseEnum().equals(RegResponseEnum.REG_OK)) {
                        Platform.runLater(() -> showOk("Пользователь успешно зарегистрирован"));
                        //  Platform.runLater(() -> switchScene("geek_cloud_auth.fxml"));

                    } else if (regResponse.getRegResponseEnum().equals(RegResponseEnum.REG_WRONG)) {
                        Platform.runLater(() -> showError("Ошибка регистрации"));
                    }
                }

            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showOk(String message) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION, message, new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        alert.setTitle("Успешно");
        alert.showAndWait();
    }

    private void showError(String error) {
        final Alert alert = new Alert(Alert.AlertType.ERROR, error, new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
        alert.setTitle("Ошибка!");
        alert.showAndWait();
    }

    private void switchScene(String viewName) {
        thread.interrupt();
        Scene scene;
        FXMLLoader loader = new FXMLLoader(getClass().getResource(viewName));
        try {
           Parent root = loader.load();
           scene = new Scene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Stage stage = (Stage)((Node) thisEvent.getSource()).getScene().getWindow();
  //      stage = new Stage();
        stage.setTitle("Cloud Client");
        stage.setScene(scene);

        stage.show();

    }


}
