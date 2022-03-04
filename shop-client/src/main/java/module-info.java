module com.example.shopclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    opens com.pwste.shopclient to javafx.fxml;
    exports com.pwste.shopclient;
    exports com.pwste.shopclient.controllers;
    opens com.pwste.shopclient.controllers to javafx.fxml;
    exports com.pwste.shopclient.models;
    opens com.pwste.shopclient.models to javafx.fxml;
    exports com.pwste.shopclient.models.objects;
    opens com.pwste.shopclient.models.objects to javafx.fxml;
}