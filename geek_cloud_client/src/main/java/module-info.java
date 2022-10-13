module com.gb.geek_cloud_client  {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.netty.all;
    requires io.netty.codec;
    requires com.gb.common_source;


    opens com.gb.geek_cloud_client to javafx.fxml;
    exports com.gb.geek_cloud_client;
}