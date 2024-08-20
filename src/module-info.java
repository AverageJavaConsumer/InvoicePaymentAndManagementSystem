module main.java.com.example.fifo {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    //requires org.kordamp.bootstrapfx.core;

    //requires com.almasb.fxgl.all;

    opens main.java.com.example.fifoproject to javafx.fxml;
    exports main.java.com.example.fifoproject;
}