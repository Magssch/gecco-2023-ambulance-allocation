module ambulanceallocation {
    requires com.sothawo.mapjfx;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.slf4j;
    requires com.google.gson;

    opens no.ntnu.ambulanceallocation.simulation to javafx.fxml, javafx.graphics;
}
