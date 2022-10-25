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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

@Slf4j
public class StartWindowAuthReg implements Initializable {
    public TextField loginFieldAuth;
    public TextField passwordFieldAuth;
    public TextField loginFieldReg;
    public TextField passwordFieldReg;
    private ActionEvent lastEvent;
    private Stage stage;
    private Scene scene;
    private Parent root;
    private String test="null";
//    private Thread thread;
    private Network<ObjectDecoderInputStream, ObjectEncoderOutputStream> network;

    public void onAuthorization(ActionEvent actionEvent) throws IOException {
        log.debug("Войти");
        lastEvent=actionEvent;
        network.getOutputStream().writeObject(new AuthRequest(loginFieldAuth.getText(), passwordFieldAuth.getText()));
    }

    public void onRegistration(ActionEvent actionEvent) throws IOException {
        log.debug("Регистрация");
        lastEvent=actionEvent;
        network.getOutputStream().writeObject(new RegRequest(loginFieldReg.getText(), passwordFieldReg.getText()));
    }

    public void onSwitchToRegWindow(ActionEvent actionEvent) {
        log.debug("Переключить на регистрацию");
        lastEvent=actionEvent;
        Platform.runLater(() -> switchScene("geek_cloud_reg.fxml"));
    }

    public void onBack(ActionEvent actionEvent) {
        log.debug("Возврат к авторизации");
        lastEvent=actionEvent;
        Platform.runLater(() -> switchScene("geek_cloud_auth.fxml"));
    }
@Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    log.debug("Инициализация-------------------------------------------------");
//    log.debug((factory==null)?"NULL":factory.toString());
        initNetwork();
   log.debug(this.toString());
//    test = "sdfsdf";
//    log.debug(test);
}
    private void initNetwork() {
            network = Network.getInstance();
            ThreadChannel.startInstance(this::auth);
//            if (factory==null) {
//                factory = DaemonThreadFactory.getInstance();
//                Thread thread = factory.getThread(this::auth, "cloud-auth-read-thread");
//                thread.start();
//
//            }

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
                        Platform.runLater(() -> switchScene("geek_cloud_auth.fxml"));
                        break;
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

        FXMLLoader loader = new FXMLLoader(getClass().getResource(viewName));
        try {
            root = loader.load();
            scene = new Scene(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage = (Stage)((Node)lastEvent.getSource()).getScene().getWindow();
        stage.setTitle("Cloud Client");
        stage.setScene(scene);
        stage.show();

    }


}
