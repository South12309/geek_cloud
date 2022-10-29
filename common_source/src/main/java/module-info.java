module com.gb.common_source  {
    requires lombok;
    requires org.slf4j;

    exports com.gb.common_source;
    exports com.gb.common_source.model;
    exports com.gb.common_source.model.auth;
    exports com.gb.common_source.model.reg;
    exports com.gb.common_source.model.file;
}