/*
 * Copyright (C) 2026 Matthias Grimm
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import TimeTracker.data.Database;

/**
 * Master data container class of the application
 * 
 * @author Matthias Grimm
 */
public class Registry
{
    private static volatile Registry instance = null;   // Singleton object instance 

    private Database database;
    private GlobalHotkey hotkey;
    private ExecutorService threadExecutor = Executors.newFixedThreadPool(10);
    
    // Configuration
    private Path cfgDBPath;
    private int cfgBreakTime;
    private GlobalHotkey cfgHotkey;

    /**
     * This method is the global access to the Registry object, which is a Singleton
     * 
     * @return  Singleton instance of Registry object
     */
    @SuppressWarnings("DoubleCheckedLocking")
    public static Registry get()
    {
        if (instance == null)
        {
            synchronized(Registry.class)
            {
                if (instance == null)
                    instance = new Registry();
            }
        }
        return instance;
    }

    /**
     * Singletons require a private constructor, so no further objects of this class could
     * be instanciated.
     */
    private Registry()
    {
        cfgDBPath = Paths.get(System.getProperty("user.dir"), Defaults.DB_FILE_NAME);
    }

    /**
     * Cleanup stuff before close the program
     */
    public void close()
    {
        if (hotkey != null)
            hotkey.unregister();

        threadExecutor.shutdown();
        try {
            if (!threadExecutor.awaitTermination(200, TimeUnit.MILLISECONDS))
                threadExecutor.shutdownNow();

        } catch (InterruptedException e) {
            threadExecutor.shutdownNow();
        }

        if (database != null) {
            try {
                database.close();
            } catch (SQLException e) {
                System.err.println("Database could not be closed: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the location of the SQLite database file. This is either the path
     * given on the command line via the "db" option or the default location.
     *
     * @return the database file path
     */
    public Path getDatabasePath()
    {
        return cfgDBPath;
    }

    /**
     * Sets the location of the SQLite database file, e.g. from the "db" command
     * line option.
     *
     * @param dbPath the database file path
     */
    public void setDatabasePath(Path dbPath)
    {
        this.cfgDBPath = dbPath;
    }

    /**
     * Returns the open database handle, or null if the database has not been
     * opened yet.
     *
     * @return the database handle
     */
    public Database getDatabase()
    {
        return database;
    }

    /**
     * Stores the open database handle so it can be accessed application wide and
     * loads the persisted configuration (break time and global hotkey) from it
     * into the {@code cfgBreakTime} and {@code cfgHotkey} fields. If the
     * configuration can not be read the built-in defaults are used so the
     * application stays usable.
     *
     * @param database the open database handle
     */
    public void setDatabase(Database database)
    {
        this.database = database;

        try {
            Database.Config cfg = database.readConfig();
            cfgBreakTime = cfg.breakTime();
            cfgHotkey    = configuredHotkey(cfg.hotkeyCombo());

        } catch (SQLException e) {
            System.err.println("Configuration could not be read: " + e.getMessage());
            cfgBreakTime = Defaults.DEFAULT_BREAK_TIME;
            cfgHotkey    = configuredHotkey(GlobalHotkey.DEFAULT_HOTKEY);
        }
    }

    /**
     * Builds a configuration-only {@link GlobalHotkey} that merely carries the
     * given packed combination. It has no trigger action and is never
     * registered; the live, registered hotkey is created separately.
     *
     * @param packedCombo the hotkey combination in packed form
     * @return a hotkey instance holding that combination
     */
    private static GlobalHotkey configuredHotkey(int packedCombo)
    {
        GlobalHotkey hotkey = new GlobalHotkey(null);
        hotkey.setHotkey(packedCombo);
        return hotkey;
    }

    /**
     * Returns the configured break duration in minutes.
     *
     * @return the break time
     */
    public int getBreakTime()
    {
        return cfgBreakTime;
    }

    /**
     * Returns the configuration-only hotkey carrying the persisted combination.
     *
     * @return the configured hotkey
     */
    public GlobalHotkey getHotkey()
    {
        return cfgHotkey;
    }

    public ExecutorService getExecutor() { return threadExecutor; }

    /**
     * Stores the global hotkey handler so it can be uninstalled on shutdown.
     *
     * @param hotkey the registered global hotkey handler
     */
    public void setHotkey(GlobalHotkey hotkey)
    {
        this.hotkey = hotkey;
    }
}
