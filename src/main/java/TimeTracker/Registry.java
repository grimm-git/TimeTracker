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
import TimeTracker.data.Session;
import TimeTracker.util.GlobalHotkey;

/**
 * Master data container class of the application
 * 
 * @author Matthias Grimm
 */
public class Registry
{
    private static volatile Registry instance = null;   // Singleton object instance 

    private Database dbHandle;
    private GlobalHotkey hotkey;                 // live, registered global hook
    private ExecutorService threadExecutor = Executors.newFixedThreadPool(10);
    private Session activeSession;

    // Configuration
    private Path cfgDBPath;
    private int cfgBreakTime;
    private int cfgHotkey;                        // configured hotkey in packed form
    private int cfgHideAtStart;                   // "hide at start" flag (0/1)

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

        if (dbHandle != null) {
            try {
                activeSession.finishSession();
                dbHandle.writeSession(activeSession);

                dbHandle.close();
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
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
    public Database getDbHandle()
    {
        return dbHandle;
    }

    /**
     * Stores the open database handle so it can be accessed application wide and
     * loads the persisted configuration (break time and global hotkey) from it
     * into the {@code cfgBreakTime} and {@code cfgHotkey} fields. If the
     * configuration can not be read the built-in defaults are used so the
     * application stays usable.
     *
     * @param handle the open database handle
     */
    public void setDbHandle(Database handle)
    {
        dbHandle = handle;

        try {
            activeSession = dbHandle.getLastSession();

            Database.Config cfg = dbHandle.readConfig();
            cfgBreakTime   = cfg.breakTime();
            cfgHotkey      = cfg.hotkeyCombo();
            cfgHideAtStart = cfg.hideAtStart();

        } catch (SQLException e) {
            System.err.println("Configuration could not be read: " + e.getMessage());
            cfgBreakTime   = Defaults.DEFAULT_BREAK_TIME;
            cfgHotkey      = GlobalHotkey.DEFAULT_HOTKEY;
            cfgHideAtStart = Defaults.DEFAULT_HIDE_AT_START;
        }
    }

    public Session getActiveSession()
    {
        return activeSession;
    }

    public void setActiveSession(Session active)
    {
        activeSession = active;
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
     * Applies a newly chosen break duration. The in-memory configuration is
     * updated first and can not fail; the value is then persisted to the
     * database (together with the other configuration fields) so it survives a
     * restart. If the database write fails the value is still effective for the
     * running session and the {@link SQLException} is propagated so the caller
     * can inform the user.
     *
     * @param minutes the break duration in minutes
     * @throws SQLException if the value could not be persisted
     */
    public void updateBreakTime(int minutes) throws SQLException
    {
        cfgBreakTime = minutes;

        if (dbHandle != null)
            dbHandle.writeConfig(new Database.Config(cfgBreakTime, cfgHotkey, cfgHideAtStart));
    }

    /**
     * Returns the configured global hotkey in packed form.
     *
     * @return the configured hotkey combination
     *         (see {@link GlobalHotkey#packHotkey(int, int)})
     */
    public int getHotkey()
    {
        return cfgHotkey;
    }

    /**
     * Returns whether the "hide at start" flag is enabled.
     *
     * @return true if the application should hide at start, false otherwise
     */
    public boolean isHideAtStart()
    {
        return cfgHideAtStart != 0;
    }

    /**
     * Applies a newly chosen "hide at start" setting. The in-memory
     * configuration is updated first and can not fail; the value is then
     * persisted to the database (together with the other configuration fields)
     * so it survives a restart. If the database write fails the setting is still
     * effective for the running session and the {@link SQLException} is
     * propagated so the caller can inform the user.
     *
     * @param hideAtStart true to enable hiding at start, false to disable it
     * @throws SQLException if the setting could not be persisted
     */
    public void updateHideAtStart(boolean hideAtStart) throws SQLException
    {
        cfgHideAtStart = hideAtStart ? 1 : 0;

        if (dbHandle != null)
            dbHandle.writeConfig(new Database.Config(cfgBreakTime, cfgHotkey, cfgHideAtStart));
    }

    /**
     * Stores the live, registered global hook so it can be re-targeted when the
     * combination changes and uninstalled on shutdown.
     *
     * @param hotkey the registered global hotkey handler
     */
    public void setHotkey(GlobalHotkey hotkey)
    {
        this.hotkey = hotkey;
    }

    /**
     * Applies a newly chosen global hotkey combination. The change is reflected
     * in three places: the in-memory configuration ({@link #cfgHotkey}), the
     * live registered hook (so the combination is effective immediately, without
     * reinstalling the native hook), and the database (so it survives a restart).
     * <p>
     * The in-memory and live updates happen first and can not fail; if the
     * database write fails the combination is still active for the running
     * session and the {@link SQLException} is propagated so the caller can inform
     * the user.
     *
     * @param packedHotkey the new combination in packed form
     *                    (see {@link GlobalHotkey#packHotkey(int, int)})
     * @throws SQLException if the combination could not be persisted
     */
    public void updateHotkey(int packedHotkey) throws SQLException
    {
        cfgHotkey = packedHotkey;

        if (hotkey != null)
            hotkey.setHotkey(packedHotkey);

        if (dbHandle != null)
            dbHandle.writeConfig(new Database.Config(cfgBreakTime, packedHotkey, cfgHideAtStart));
    }

    public ExecutorService getExecutor() { return threadExecutor; }
}
