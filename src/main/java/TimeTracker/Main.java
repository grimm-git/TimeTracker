/*
 * Copyright (C) 2026 Matthias Grimm <codingjoker@web.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package TimeTracker;

import java.io.IOException;
import java.nio.file.Paths;

import com.github.kwhat.jnativehook.NativeHookException;

import TimeTracker.data.Configuration;
import TimeTracker.gui.MainWindowController;
import TimeTracker.gui.SplashController;
import TimeTracker.util.GlobalHotkey;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * @author Matthias Grimm
 */
public class Main
extends Application
{
    /**
     * Runs before the GUI is created. It reads the "db" command line option,
     * which holds the path to the SQLite database file, and stores it in the
     * Registry. If the option is missing the Registry keeps its default path.
     *
     * The option is passed in JavaFX named-parameter style, e.g.
     * {@code --db=/path/to/timetracker.db}.
     */
    @Override
    public void init()
    {
        Registry Reg = Registry.get();

        String db = getParameters().getNamed().get("db");
        if (db != null && !db.isEmpty())
            Reg.getConfig().setDBPath(Paths.get(db));
    }

    @Override
    public void start(Stage stage) throws IOException
    {
        Registry Reg = Registry.get();

        setUserAgentStylesheet(STYLESHEET_MODENA);

        // Closing the main window only hides it; the application keeps running
        // in the background. Without this, hiding the last visible window would
        // make JavaFX shut down the runtime and terminate the process.
        Platform.setImplicitExit(false);

        // Graceful-termination handler: when the operating system asks the
        // process to quit (SIGTERM on shutdown/logout, SIGINT on Ctrl+C) the JVM
        // runs this shutdown hook, so the database and worker threads are closed
        // cleanly. A forced SIGKILL (kill -9) cannot be intercepted.
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> Reg.close(), "shutdown-hook"));

        SplashController sc = new SplashController();
        sc.showAndWait();

        // The application can not run without its database, so open it first.
        // On failure the user is informed and the application terminates.
        if (!Reg.initDatabase()) {
            Platform.exit();
            return;
        }

        MainWindowController mw = new MainWindowController(stage);
        stage.getIcons().add(mw.getImageResource("timetracker_16x16.png"));
        stage.getIcons().add(mw.getImageResource("timetracker_32x32.png"));
        stage.getIcons().add(mw.getImageResource("timetracker_64x64.png"));
        stage.getIcons().add(mw.getImageResource("timetracker_256x256.png"));

        Configuration Config = Reg.getConfig();

        // The main window only appears at start when "hide at start" is false.
        // When it is true then the application starts in the background and the
        // window is revealed later via the global hotkey (see below).
        if (!Config.getHideAtStart())
            stage.show();

        // Install a system-wide hotkey (CTRL+SHIFT+F10) that re-shows the window
        // even when the application has no focus. Failure to install the native
        // hook (e.g. on a Wayland session) must not prevent the app from running.
        try {
            GlobalHotkey hotkey = new GlobalHotkey(mw::handleHotkey);
            Config.setHotkey(hotkey);

        } catch (NativeHookException e) {
            Platform.exit();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        // Cleanup is done by the shutdown hook registered in start(), so it runs
        // both on a normal exit and when the OS terminates the process.
        launch(args);       // start JavaFX Thread
    }
}
