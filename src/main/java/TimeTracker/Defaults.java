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

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 *
 * @author Matthias Grimm
 */
public final class Defaults
{
    /**
     * APPLICATION_NAME is the full name of this application. It will be used whwerever a human
     * readable name is needed.
     */
    public static final String APP_NAME  = "TimeTracker";

    /**
     * The program version, revision, optional suffix and build date. These are
     * loaded from {@code /version.properties}, which is generated at build time
     * from the single source of truth in {@code gradle.properties} (see
     * build.gradle). They are shown in the splash, window title and About window
     * and are for information only.
     */
    public static final int       APP_VERSION;
    public static final int       APP_REVISION;
    public static final String    APP_SUFFIX;
    public static final LocalDate APP_DATE;

    static {
        Properties p = new Properties();
        try (InputStream in = Defaults.class.getResourceAsStream("/version.properties")) {
            if (in != null)
                p.load(in);
            else
                System.err.println("version.properties not found on the classpath; using fallback version.");

        } catch (IOException e) {
            System.err.println("version.properties could not be read: " + e.getMessage());
        }

        APP_VERSION  = parseIntOrDefault(p.getProperty("version"), 0);
        APP_REVISION = parseIntOrDefault(p.getProperty("revision"), 0);
        APP_SUFFIX   = p.getProperty("suffix", "").trim();
        APP_DATE     = parseDateOrDefault(p.getProperty("date"));
    }

    /**
     * Parses an integer, returning a fallback when the value is missing or not a
     * number (e.g. when the resource was not token-filtered by the build).
     */
    private static int parseIntOrDefault(String value, int fallback)
    {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    /**
     * DEFAULT_FIRST_BREAK is the time in a session that must be elappsed
     * before the break time is considered.
     */
    public static final int DEFAULT_FIRST_BREAK = 4*60;
     
    /**
     * DEFAULT_BREAK_TIME is the break duration in minutes used to seed the
     * configuration table when a database is created for the first time.
     */
    public static final int DEFAULT_BREAK_TIME = 45;

    /**
     * DEFAULT_HIDE_AT_START is the seeded value of the "hide at start" flag
     * stored in the configuration table (0 = disabled, 1 = enabled).
     */
    public static final boolean DEFAULT_HIDE_AT_START = false;

    /**
     * Parses an ISO date (yyyy-MM-dd), falling back to the current date when the
     * value is missing or malformed.
     */
    private static LocalDate parseDateOrDefault(String value)
    {
        try {
            return LocalDate.parse(value.trim());
        } catch (java.time.format.DateTimeParseException | NullPointerException e) {
            return LocalDate.now();
        }
    }

    /**
     * DB_FILE_NAME is the default file name of the SQLite database. It is used
     * when no database path is given on the command line.
     */
    public static final String    DB_FILE_NAME = "timetracker.db";

    /**
     * BANK_HOLIDAYS_FILE_NAME is the file name of the plain text list of bank
     * holidays. It contains one date per line in ISO format (YYYY-MM-DD) and is
     * looked up in the working directory next to the database file.
     */
    public static final String    BANK_HOLIDAYS_FILE = "bankholidays.db";

    public static String getVersionString()
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        return String.format("%d.%d%s (%s)",
                APP_VERSION, APP_REVISION, APP_SUFFIX, APP_DATE.format(formatter));
    }

}
