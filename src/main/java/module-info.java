module ambulanceallocation {
    requires com.sothawo.mapjfx;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.slf4j;
    requires org.json;

    opens no.ntnu.ambulanceallocation.simulation to javafx.fxml, javafx.graphics;
}
