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
package TimeTracker.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;

import TimeTracker.Defaults;
import TimeTracker.Registry;
import TimeTracker.util.GlobalHotkey;
import TimeTracker.util.Language;

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

    /**
     * Creates a Database bound to the given file location and makes sure the
     * database and its schema exist. If the database file is missing it is
     * created and initialised with the schema.
     *
     * @param dbPath the location of the SQLite database file
     * @throws SQLException if the database can not be opened or created
     */
    public Database(Path arg) throws SQLException
    {
        this.dbPath = arg;

        // Checks the existance of the SQL database file and creates it if it doesn't
        boolean exists = Files.exists(dbPath);
        if (!exists) {
            // Create all missing directories for the SQLite database file.
            try {
                Path parent = dbPath.toAbsolutePath().getParent();
                if (parent != null)
                    Files.createDirectories(parent);

            } catch (java.io.IOException e) {
                throw new SQLException(e.getLocalizedMessage(), e);
            }
        }
            
        // The schema is created with IF NOT EXISTS on every open, so databases
        // created by earlier versions gain newly added tables (e.g. config) transparently.
        Connection CXN = openDatabase();
        createSchema(CXN);
        readSession(CXN);
        readConfig(CXN);
        CXN.close();
    }

    public void updateDatabase() throws SQLException
    {
        Connection CXN = openDatabase();
        writeSession(CXN);
        writeConfig(CXN);
        CXN.close();
    }

    public void updateSession() throws SQLException
    {
        Connection CXN = openDatabase();
        writeSession(CXN);
        CXN.close();
    }

    public Connection openDatabase() throws SQLException
    {
        // Opening the connection creates the file if it does not exist yet.
        Connection CXN = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

        try (Statement stmt = CXN.createStatement()) {
            // WAL mode gives atomic, crash safe writes so that a crash while a
            // session is being written can never corrupt the database.
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
        }

        return CXN;
    }
 
    /**
     * Reads up to the last ten finished sessions from the database, most recent
     * first. The current session is excluded
     *
     * @return an ArrayList of the last (at most ten) finished sessions, newest
     *         first; empty if the database holds no finished sessions
     * @throws SQLException if the sessions can not be read
     */
    public ArrayList<Session> getSessionLog() throws SQLException
    {
        ArrayList<Session> sessions = new ArrayList<>();

        String sql = "SELECT id, start, end, hadbreak FROM sessions "
                   + "ORDER BY id DESC LIMIT 10 OFFSET 1";

        Connection CXN = openDatabase();
        try (Statement stmt = CXN.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                LocalDateTime start = toLocalDateTime(rs.getLong("start"));
                LocalDateTime end   = toLocalDateTime(rs.getLong("end"));

                Session session = new Session(id, start, end);
                session.setBreak(rs.getInt("hadbreak") != 0);
                sessions.add(session);
            }
        }
        CXN.close();

        return sessions;
    }

    /**
     * Writes every recorded session to the given file in CSV format, ordered
     * from the most recent session to the oldest. The file is created if it does
     * not exist and overwritten if it does. Each row holds the session id, the
     * day of week, the date, the start and end time and the raw duration; the
     * end and duration are left empty for a session that has not finished yet.
     *
     * @param filePath the CSV file to write the sessions to
     * @throws SQLException if the sessions can not be read from the database
     * @throws IOException  if the file can not be written
     */
    public void dumpAllSessions(Path filePath) throws SQLException, IOException
    {
        Registry Reg = Registry.get();
        Language i18n = Reg.getI18N();

        String sql = "SELECT id, start, end FROM sessions ORDER BY id DESC";

        Connection CXN = openDatabase();
        try (BufferedWriter writer = Files.newBufferedWriter(filePath);
             Statement stmt = CXN.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            writer.write("id,day,date,start,end,duration");
            writer.newLine();

            while (rs.next()) {
                int id = rs.getInt("id");
                LocalDateTime start = toLocalDateTime(rs.getLong("start"));

                long endMillis = rs.getLong("end");
                LocalDateTime end = toLocalDateTime(endMillis);

                String day      = start.getDayOfWeek()
                                       .getDisplayName(TextStyle.FULL, i18n.locale());
                String date     = i18n.localDate(start.toLocalDate());
                String startStr = i18n.localTime(start.toLocalTime());
                String endStr   = i18n.localTime(end.toLocalTime());
                String duration = formatDuration(Duration.between(start, end));

                writer.write(csv(String.valueOf(id))
                           + "," + csv(day)
                           + "," + csv(date)
                           + "," + csv(startStr)
                           + "," + csv(endStr)
                           + "," + csv(duration));
                writer.newLine();
            }
        } finally {
            CXN.close();
        }
    }

    /**
     * Formats a Duration as HH:mm, e.g. a duration of 90 minutes becomes
     * "01:30". Hours are not capped at 24 so long sessions stay readable.
     *
     * @param duration the duration to format
     * @return the duration rendered as HH:mm
     */
    private static String formatDuration(Duration duration)
    {
        long hours   = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * Escapes a value for CSV output. A field containing a comma, a double
     * quote or a line break is wrapped in double quotes with any embedded
     * double quotes doubled, as required by RFC 4180.
     *
     * @param value the raw field value
     * @return the value safe to place between commas in a CSV row
     */
    private static String csv(String value)
    {
        if (value.contains(",") || value.contains("\"")
         || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    /******************************************************************************
     *                               Private methods                              *
     ******************************************************************************/

    /**
     * Creates the database schema. The sessions table holds one row per
     * recorded session with a start and an end time stamp, both stored as epoch
     * milliseconds (UTC). The index on the start column speeds up date and time
     * period queries. The single-row config table holds the application
     * configuration; its CHECK constraint pins it to exactly one row (id = 1)
     * and it is seeded with the default break time and hotkey combination, an
     * existing configuration being left untouched.
     *
     * @param CNX Open connection to database
     * @throws SQLException if the schema can not be created
     */
    private void createSchema(Connection CXN) throws SQLException
    {
        try (Statement stmt = CXN.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sessions ("
              + "    id       INTEGER PRIMARY KEY AUTOINCREMENT, "
              + "    start    INTEGER NOT NULL, "
              + "    end      INTEGER, "
              + "    hadbreak INTEGER NOT NULL DEFAULT 0"
              + ")");

            stmt.execute(
                "CREATE INDEX IF NOT EXISTS idx_sessions_start "
              + "ON sessions(start)");

            // hadbreak records whether the configured break was inserted into a
            // session; add it to sessions tables created by earlier versions.
            if (!columnExists(CXN, "sessions", "hadbreak"))
                stmt.execute("ALTER TABLE sessions ADD COLUMN hadbreak INTEGER NOT NULL DEFAULT 0");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS config ("
              + "    id          INTEGER PRIMARY KEY CHECK (id = 1), "
              + "    breaklength INTEGER NOT NULL, "
              + "    hotkey      INTEGER NOT NULL, "
              + "    hideatstart INTEGER NOT NULL DEFAULT 0,"
              + "    wdsaturday  INTEGER NOT NULL DEFAULT 0,"
              + "    wdsunday    INTEGER NOT NULL DEFAULT 0,"
              + "    insertbreak INTEGER NOT NULL DEFAULT 0,"
              + "    breaktime   REAL"
              + ")");

            // Bring config tables created by earlier versions up to date by
            // adding columns introduced later. On a freshly created table the
            // column is already present, so the migration is guarded to run
            // only when it is actually missing.
            if (!columnExists(CXN, "config", "hideatstart"))
                stmt.execute("ALTER TABLE config ADD COLUMN hideatstart INTEGER NOT NULL DEFAULT 0");
            if (!columnExists(CXN, "config", "wdsaturday"))
                stmt.execute("ALTER TABLE config ADD COLUMN wdsaturday INTEGER NOT NULL DEFAULT 0");
            if (!columnExists(CXN, "config", "wdsunday"))
                stmt.execute("ALTER TABLE config ADD COLUMN wdsunday INTEGER NOT NULL DEFAULT 0");
            if (!columnExists(CXN, "config", "breaklength"))
                stmt.execute("ALTER TABLE config RENAME COLUMN breaktime TO breaklength");
            if (columnExists(CXN, "config", "hasbreak"))
                stmt.execute("ALTER TABLE config RENAME COLUMN hasbreak TO insertbreak");
            else if (!columnExists(CXN, "config", "insertbreak"))
                stmt.execute("ALTER TABLE config ADD COLUMN insertbreak INTEGER NOT NULL DEFAULT 0");
            if (!columnExists(CXN, "config", "breaktime"))
                stmt.execute("ALTER TABLE config ADD COLUMN breaktime REAL");

            // breaktime changed meaning: it used to hold a wall-clock time (an ISO
            // string, later the seconds of day) and now holds the hours since the
            // session start as a REAL. Old values can not be converted, so reset
            // any left behind in a previous format to the default.
            try (PreparedStatement upd = CXN.prepareStatement(
                    "UPDATE config SET breaktime = ? WHERE id = 1 AND typeof(breaktime) IN ('text', 'integer')")) {
                upd.setFloat(1, Defaults.DEFAULT_BREAK_TIME);
                upd.executeUpdate();
            }
        }

        try (PreparedStatement stmt = CXN.prepareStatement(
                "INSERT OR IGNORE INTO config (id, breaklength, hotkey, hideatstart, wdsaturday, wdsunday, insertbreak, breaktime) VALUES (1, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, Defaults.DEFAULT_BREAK_LENGTH);
            stmt.setInt(2, GlobalHotkey.DEFAULT_HOTKEY);
            stmt.setInt(3, Defaults.DEFAULT_HIDE_AT_START ? 1 : 0);
            stmt.setInt(4, 0);
            stmt.setInt(5, 0);
            stmt.setInt(6, 0);
            stmt.setFloat(7, Defaults.DEFAULT_BREAK_TIME);
            stmt.executeUpdate();
        }
    }

    /**
     * Checks whether the given table already has a column of the given name,
     * using SQLite's {@code PRAGMA table_info}. Used to make column-adding
     * schema migrations idempotent.
     *
     * @param CXN    Open database connection
     * @param table  the table to inspect
     * @param column the column name to look for
     * @return true if the column exists, false otherwise
     * @throws SQLException if the table can not be inspected
     */
    private boolean columnExists(Connection CXN, String table, String column) throws SQLException
    {
        try (Statement stmt = CXN.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {

            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name")))
                    return true;
            }
        }
        return false;
    }

    /**
     * Reads the configuration row from the database. As {@link #createSchema()}
     * seeds the row on every open, a row is normally always present; should it
     * be missing, an SQLException is thrown.
     *
     * @param  CXN  Open database connection
     * @throws SQLException if the configuration can not be read 
     */
    private void readConfig(Connection CXN) throws SQLException
    {
        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();

        String sql = "SELECT insertbreak, breaktime, breaklength, hotkey, hideatstart, wdsaturday, wdsunday FROM config WHERE id = 1";

        try (Statement stmt = CXN.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                Config.setHasBreak(rs.getInt("insertbreak") == 0 ? false : true);
                Config.setBreakLength(rs.getInt("breaklength"));
                Config.setHotkey(rs.getInt("hotkey"));
                Config.setHideAtStart(rs.getInt("hideatstart") == 0 ? false : true);
                Config.setWDSaturday(rs.getInt("wdsaturday") == 0 ? false : true);
                Config.setWDSunday(rs.getInt("wdsunday") == 0 ? false : true);

                // breaktime is stored as the hours since the session start; it is
                // nullable, so keep the in-memory default when it is unset.
                float breakTime = rs.getFloat("breaktime");
                if (!rs.wasNull())
                    Config.setBreakTime(breakTime);
            }
        }
    }

    /**
     * Writes the configuration back to the single-row config table. The upsert
     * updates the existing row (id = 1) or, should it be missing, inserts it, so
     * the configuration is persisted regardless of the prior table state.
     * The database will be opened if necessary and closed again afterwards.
     *
     * @param  CXN  Open connection to database
     * @throws SQLException if the configuration can not be written
     */
    private void writeConfig(Connection CXN) throws SQLException
    {
        Registry Reg = Registry.get();
        Configuration Config = Reg.getConfig();
        
        if (Config.isDirty()) {
            String sql = "INSERT INTO config (id, breaklength, hotkey, hideatstart, wdsaturday, wdsunday, insertbreak, breaktime) VALUES (1, ?, ?, ?, ?, ?, ?, ?) "
                       + "ON CONFLICT(id) DO UPDATE SET breaklength = excluded.breaklength, "
                       + "hotkey = excluded.hotkey, hideatstart = excluded.hideatstart, "
                       + "wdsaturday = excluded.wdsaturday, wdsunday = excluded.wdsunday, "
                       + "insertbreak = excluded.insertbreak, breaktime = excluded.breaktime";

            try (PreparedStatement stmt = CXN.prepareStatement(sql)) {
                stmt.setInt(1, Config.getBreakLength());
                stmt.setInt(2, Config.getHotkey());
                stmt.setInt(3, Config.getHideAtStart() ? 1 : 0);
                stmt.setInt(4, Config.getWDSaturday() ? 1 : 0);
                stmt.setInt(5, Config.getWDSunday() ? 1 : 0);
                stmt.setInt(6, Config.hasBreak() ? 1 : 0);

                stmt.setFloat(7, Config.getBreakTime());

                stmt.executeUpdate();
            }
        }
    }

    /**
     * Reads the most recently recorded session from the database and sets it as
     * active session in the Registry. The newest session is the one with the
     * highest id, i.e. the row that was inserted last.
     *
     * @param  CXN  OPen connection to database
     * @throws SQLException if the session can not be read
     */
    private void readSession(Connection CXN) throws SQLException
    {
        Registry Reg = Registry.get();

        String sql = "SELECT id, start, end, hadbreak FROM sessions ORDER BY id DESC LIMIT 1";

        try (Statement stmt = CXN.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int id = rs.getInt("id");
                LocalDateTime start = toLocalDateTime(rs.getLong("start"));
                LocalDateTime end   = toLocalDateTime(rs.getLong("end"));

                Session session = new Session(id, start, end);
                session.setBreak(rs.getInt("hadbreak") != 0);
                Reg.setSession(session);
            }
        }
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
    private void writeSession(Connection CXN) throws SQLException
    {
        Registry Reg = Registry.get();

        Session session = Reg.getSession();
        long start = toEpochMillis(session.getSessionStart());
        long end   = toEpochMillis(session.getSessionEnd());
        int  id    = session.getID();

        String sql = (id != 0)
                   ? "UPDATE sessions SET start = ?, end = ?, hadbreak = ? WHERE id = ?"
                   : "INSERT INTO sessions (start, end, hadbreak) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = CXN.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, start);
            stmt.setLong(2, end);
            stmt.setInt(3, session.hadBreak() ? 1 : 0);

            if (id != 0) {
                stmt.setInt(4, id);
                stmt.executeUpdate();
            } else {
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next())
                        session.setID(keys.getInt(1));
                }
            }
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
  
}
