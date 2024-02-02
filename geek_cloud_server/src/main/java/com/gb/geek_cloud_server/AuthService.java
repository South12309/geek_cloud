package com.gb.geek_cloud_server;

import com.gb.common_source.model.auth.AuthRequest;
import com.gb.common_source.model.auth.AuthResponse;
import com.gb.common_source.model.reg.RegRequest;
import com.gb.common_source.model.reg.RegResponse;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;

public interface AuthService extends Closeable {

    RegResponse registration(RegRequest regRequest) throws SQLException;
    AuthResponse authorization(AuthRequest authRequest);
    public String getDirNameLogin();

    void run();

    @Override
    void close() throws IOException;
}
