package com.gb.geek_cloud_server;

import com.gb.common_source.model.auth.AuthRequest;
import com.gb.common_source.model.auth.AuthResponse;
import com.gb.common_source.model.auth.AuthResponseEnum;
import com.gb.common_source.model.reg.RegRequest;
import com.gb.common_source.model.reg.RegResponse;
import com.gb.common_source.model.reg.RegResponseEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.*;

@Slf4j
public class SQLAuthService implements AuthService {
    private static Connection connection;
    private String dirNameLogin;

    @Override
    public RegResponse registration(RegRequest regRequest) {
        try (PreparedStatement preparedStatement = connection.
                    prepareStatement("INSERT INTO users (login, password) VALUES (?, ?);")) {
            preparedStatement.setString(1, regRequest.getLogin());
            preparedStatement.setString(2, regRequest.getPassword());
            preparedStatement.executeUpdate();
            return new RegResponse(RegResponseEnum.REG_OK);
        } catch (SQLException e) {
            e.printStackTrace();
            return new RegResponse(RegResponseEnum.REG_WRONG);
        }
    }

    @Override
    public AuthResponse authorization(AuthRequest authRequest) {
        try (PreparedStatement preparedStatement = connection.
                    prepareStatement("SELECT login FROM users WHERE login=? AND password=?;")) {
            preparedStatement.setString(1, authRequest.getLogin());
            preparedStatement.setString(2, authRequest.getPassword());
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            dirNameLogin = resultSet.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();

        }
        if (dirNameLogin == null) {
            return new AuthResponse(AuthResponseEnum.AUTH_WRONG);
        } else {
            return new AuthResponse(AuthResponseEnum.AUTH_OK);
        }

    }

    @Override
    public void run() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:cloud.db");
            log.debug("Подключение к базе выполнено.");
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Ошибка при подключении к базе.");
        }

    }

    @Override
    public void close() throws IOException {
        try {
            if (connection != null)
                connection.close();
            log.debug("Отключение от базы выполнено.");
        } catch (SQLException e) {
            log.error("Ошибка при отключении от базе.");
            e.printStackTrace();
        }
    }

    public String getDirNameLogin() {
        if (dirNameLogin == null)
            return "";
        else
            return dirNameLogin;
    }
}
