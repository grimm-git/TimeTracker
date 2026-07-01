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
package TimeTracker.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

import TimeTracker.Defaults;
import TimeTracker.GlobalHotkey;

/**
 * Database manages the SQLite database that stores the recorded sessions.
 *
 * Each session is stored with a start and an end time stamp. The time stamps
 * are kept as epoch milliseconds (UTC) so that range queries and ordering are
 * simple integer comparisons. An index on the start column allows fast lookups
 * of a given date or time period without reading the whole table into memory.
 *
 * @author Matthias Grimm
 */
public class Database
{
    private final Path dbPath;
    private Connection dbCNX;

    /**
     * Creates a Database bound to the given file location and makes sure the
     * database and its schema exist. If the database file is missing it is
     * created and initialised with the schema.
     *
     * @param dbPath the location of the SQLite database file
     * @throws SQLException if the database can not be opened or created
     */
    public Database(Path dbPath) throws SQLException
    {
        this.dbPath = dbPath;
        initDatabase();
    }

    /**
     * Checks for the existence of the SQLite database file and creates it with
     * the required schema if it is missing. When the file already exists the
     * connection is simply opened. The schema is created with IF NOT EXISTS so
     * that calling this method on an existing database is harmless.
     *
     * @throws SQLException if the database can not be opened or the schema can
     *                      not be created
     */
    private void initDatabase() throws SQLException
    {
        boolean exists = Files.exists(dbPath);

        // Opening the connection creates the file if it does not exist yet.
        dbCNX = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

        try (Statement stmt = dbCNX.createStatement()) {
            // WAL mode gives atomic, crash safe writes so that a crash while a
            // session is being written can never corrupt the database.
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
        }

        if (!exists) {
            // Create all missing directories for the SQLite database file.
            try {
                Path parent = dbPath.toAbsolutePath().getParent();
                if (parent != null)
                    Files.createDirectories(parent);

            } catch (java.io.IOException e) {
                throw new SQLException("Could not create directory for database: " + dbPath, e);
            }
        }

        // The schema is created with IF NOT EXISTS on every open, so databases
        // created by earlier versions gain newly added tables (e.g. config)
        // transparently.
        createSchema();
    }

    /**
     * Creates the database schema. The sessions table holds one row per
     * recorded session with a start and an end time stamp, both stored as epoch
     * milliseconds (UTC). The index on the start column speeds up date and time
     * period queries. The single-row config table holds the application
     * configuration; its CHECK constraint pins it to exactly one row (id = 1)
     * and it is seeded with the default break time and hotkey combination, an
     * existing configuration being left untouched.
     *
     * @throws SQLException if the schema can not be created
     */
    private void createSchema() throws SQLException
    {
        try (Statement stmt = dbCNX.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sessions ("
              + "    id    INTEGER PRIMARY KEY AUTOINCREMENT, "
              + "    start INTEGER NOT NULL, "
              + "    end   INTEGER"
              + ")");

            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_sessions_start "
              + "ON sessions(start)");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS config ("
              + "    id        INTEGER PRIMARY KEY CHECK (id = 1), "
              + "    breaktime INTEGER NOT NULL, "
              + "    hotkey    INTEGER NOT NULL"
              + ")");
        }

        try (PreparedStatement stmt = dbCNX.prepareStatement(
                "INSERT OR IGNORE INTO config (id, breaktime, hotkey) VALUES (1, ?, ?)")) {
            stmt.setInt(1, Defaults.DEFAULT_BREAK_TIME);
            stmt.setInt(2, GlobalHotkey.DEFAULT_HOTKEY);
            stmt.executeUpdate();
        }
    }

    /**
     * Returns the open connection to the database.
     *
     * @return the database connection
     */
    public Connection getConnection()
    {
        return dbCNX;
    }

    /**
     * Immutable snapshot of the application configuration as stored in the
     * single-row {@code config} table.
     */
    public static final class Config
    {
        private final int breakTime;
        private final int hotkeyCombo;

        /**
         * @param breakTime   the break duration in minutes
         * @param hotkeyCombo the global hotkey combination in packed form
         *                    (see {@link GlobalHotkey#packHotkey(int, int)})
         */
        public Config(int breakTime, int hotkeyCombo)
        {
            this.breakTime = breakTime;
            this.hotkeyCombo = hotkeyCombo;
        }

        /** @return the break duration in minutes */
        public int breakTime()
        {
            return breakTime;
        }

        /** @return the global hotkey combination in packed form */
        public int hotkeyCombo()
        {
            return hotkeyCombo;
        }
    }

    /**
     * Reads the configuration row from the database. As {@link #createSchema()}
     * seeds the row on every open, a row is normally always present; should it
     * be missing, the built-in defaults are returned instead.
     *
     * @return the stored configuration
     * @throws SQLException if the configuration can not be read
     */
    public Config readConfig() throws SQLException
    {
        String sql = "SELECT breaktime, hotkey FROM config WHERE id = 1";

        try (Statement stmt = dbCNX.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next())
                return new Config(Defaults.DEFAULT_BREAK_TIME, GlobalHotkey.DEFAULT_HOTKEY);

            return new Config(rs.getInt("breaktime"), rs.getInt("hotkey"));
        }
    }

    /**
     * Writes the configuration back to the single-row config table. The upsert
     * updates the existing row (id = 1) or, should it be missing, inserts it, so
     * the configuration is persisted regardless of the prior table state.
     *
     * @param config the configuration to store
     * @throws SQLException if the configuration can not be written
     */
    public void writeConfig(Config config) throws SQLException
    {
        String sql = "INSERT INTO config (id, breaktime, hotkey) VALUES (1, ?, ?) "
                   + "ON CONFLICT(id) DO UPDATE SET breaktime = excluded.breaktime, "
                   + "hotkey = excluded.hotkey";

        try (PreparedStatement stmt = dbCNX.prepareStatement(sql)) {
            stmt.setInt(1, config.breakTime());
            stmt.setInt(2, config.hotkeyCombo());
            stmt.executeUpdate();
        }
    }

    /**
     * Reads the most recently recorded session from the database and returns it
     * as a Session object. The newest session is the one with the highest id,
     * i.e. the row that was inserted last. A session that has not been finished
     * yet (NULL end time stamp) is returned with LocalDateTime.MIN as its end,
     * matching the convention used by the Session class.
     *
     * @return the last session, or null if the database contains no sessions
     * @throws SQLException if the session can not be read
     */
    public Session getLastSession() throws SQLException
    {
        String sql = "SELECT start, end FROM sessions ORDER BY id DESC LIMIT 1";

        try (Statement stmt = dbCNX.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next())
                return null;

            LocalDateTime start = toLocalDateTime(rs.getLong("start"));

            long endMillis = rs.getLong("end");
            LocalDateTime end = rs.wasNull() ? LocalDateTime.MIN
                                             : toLocalDateTime(endMillis);

            return new Session(start, end);
        }
    }

    /**
     * Reads up to the last ten finished sessions from the database, most recent
     * first. The current, unfinished session (its end time stamp is NULL) is
     * excluded, so every returned session has a real end time.
     *
     * @return an ArrayList of the last (at most ten) finished sessions, newest
     *         first; empty if the database holds no finished sessions
     * @throws SQLException if the sessions can not be read
     */
    public ArrayList<Session> getSessionLog() throws SQLException
    {
        String sql = "SELECT start, end FROM sessions "
                   + "WHERE end IS NOT NULL ORDER BY id DESC LIMIT 10";

        ArrayList<Session> sessions = new ArrayList<>();

        try (Statement stmt = dbCNX.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                LocalDateTime start = toLocalDateTime(rs.getLong("start"));
                LocalDateTime end   = toLocalDateTime(rs.getLong("end"));
                sessions.add(new Session(start, end));
            }
        }

        return sessions;
    }

    /**
     * Writes a Session back to the database. The last recorded session decides
     * how the session is stored: if the last session is not finished yet (its
     * end time stamp is NULL) it is overwritten with the given session.
     * Otherwise, or when the database is still empty, the given session is
     * appended as a new row and becomes the new last session.
     *
     * An unfinished session (end equal to LocalDateTime.MIN) is stored with a
     * NULL end time stamp, matching the convention used by getLastSession.
     *
     * @param session the session to write
     * @throws SQLException if the session can not be written
     */
    public void writeSession(Session session) throws SQLException
    {
        long start = toEpochMillis(session.getSessionStart());
        boolean finished = !session.getSessionEnd().equals(LocalDateTime.MIN);
        long end = finished ? toEpochMillis(session.getSessionEnd()) : 0L;

        Long overwriteId = getUnfinishedLastSessionId();

        String sql = (overwriteId != null)
                   ? "UPDATE sessions SET start = ?, end = ? WHERE id = ?"
                   : "INSERT INTO sessions (start, end) VALUES (?, ?)";

        try (PreparedStatement stmt = dbCNX.prepareStatement(sql)) {
            stmt.setLong(1, start);

            if (finished)
                stmt.setLong(2, end);
            else
                stmt.setNull(2, Types.INTEGER);

            if (overwriteId != null)
                stmt.setLong(3, overwriteId);

            stmt.executeUpdate();
        }
    }

    /**
     * Returns the id of the last recorded session if that session is not
     * finished yet, i.e. its end time stamp is NULL. If the last session is
     * finished, or the database contains no sessions, null is returned.
     *
     * @return the id of the unfinished last session, or null
     * @throws SQLException if the database can not be queried
     */
    private Long getUnfinishedLastSessionId() throws SQLException
    {
        String sql = "SELECT id, end FROM sessions ORDER BY id DESC LIMIT 1";

        try (Statement stmt = dbCNX.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next())
                return null;

            long id = rs.getLong("id");
            rs.getLong("end");

            return rs.wasNull() ? id : null;
        }
    }

    /**
     * Converts a local LocalDateTime into epoch milliseconds (UTC) for storage
     * in the database, using the system default time zone.
     *
     * @param dateTime the local date and time
     * @return the corresponding time stamp in epoch milliseconds (UTC)
     */
    private static long toEpochMillis(LocalDateTime dateTime)
    {
        return dateTime.atZone(ZoneId.systemDefault())
                       .toInstant()
                       .toEpochMilli();
    }

    /**
     * Converts epoch milliseconds (UTC) as stored in the database into a local
     * LocalDateTime using the system default time zone.
     *
     * @param epochMillis the time stamp in epoch milliseconds (UTC)
     * @return the corresponding local date and time
     */
    private static LocalDateTime toLocalDateTime(long epochMillis)
    {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis),
                                       ZoneId.systemDefault());
    }

    /**
     * Closes the database connection.
     *
     * @throws SQLException if the connection can not be closed
     */
    public void close() throws SQLException
    {
        if (dbCNX != null && !dbCNX.isClosed()) {
            dbCNX.close();
        }
    }
}
