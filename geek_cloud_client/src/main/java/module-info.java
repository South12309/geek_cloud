module com.gb.geek_cloud_client {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.gb.geek_cloud_client to javafx.fxml;
    exports com.gb.geek_cloud_client;
}