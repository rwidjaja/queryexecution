module com.ubuntu {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;

    opens com.ubuntu to javafx.fxml;
    exports com.ubuntu;
}
