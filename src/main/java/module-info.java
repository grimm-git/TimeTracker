module TimeTracker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.github.kwhat.jnativehook;
    requires org.xerial.sqlitejdbc;
    requires java.rmi;
    requires javafx.graphics;   // JDBC driver, loaded as a java.sql.Driver service

    // JavaFX (and JNativeHook) reflect into these packages at runtime; without
    // the opens the app links fine but throws IllegalAccessException at launch,
    // view construction or native-hook registration.
    opens TimeTracker      to javafx.graphics;               // reflective launch of Main (Application)
    opens TimeTracker.util to com.github.kwhat.jnativehook;  // instantiates our library locator
    opens TimeTracker.gui  to javafx.fxml;                   // @FXML field/controller injection
    opens TimeTracker.data to javafx.base;                   // PropertyValueFactory reads Session getters
}
