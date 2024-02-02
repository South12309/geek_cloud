module com.gb.geek_cloud_client  {
    requires java.sql;
    requires javafx.controls;
    requires javafx.fxml;
    requires io.netty.all;
    requires io.netty.codec;
    requires lombok;
    requires slf4j.reload4j;
    requires org.slf4j;
    requires com.gb.common_source;


    opens com.gb.geek_cloud_client to javafx.fxml;
    exports com.gb.geek_cloud_client;
}