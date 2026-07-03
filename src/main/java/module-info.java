module TimeTracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.github.kwhat.jnativehook;
    requires org.xerial.sqlitejdbc;   // JDBC driver, loaded as a java.sql.Driver service

    // JavaFX reflects into these packages at runtime; without the opens the app
    // links fine but throws IllegalAccessException at launch/view construction.
    opens TimeTracker      to javafx.graphics,   // reflective launch of Main (Application)
                              com.github.kwhat.jnativehook;  // instantiates our library locator
    opens TimeTracker.gui  to javafx.fxml;      // @FXML field/controller injection
    opens TimeTracker.data to javafx.base;      // PropertyValueFactory reads Session getters
}
