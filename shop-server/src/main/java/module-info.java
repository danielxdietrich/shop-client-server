module com.example.shopserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.javalin;
    requires javax.servlet.api;
    requires org.slf4j;

    exports com.pwste.shopserver;
    exports com.pwste.shopserver.controllers;
    opens com.pwste.shopserver.controllers to javafx.fxml;
}