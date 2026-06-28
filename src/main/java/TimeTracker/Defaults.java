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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
     * APPLICATION_VERSION is a global constant containing the program version and date. It will
     * be shown in the About window. It is for information only.
     */
    public static final int       APP_VERSION = 0;
    public static final int       APP_REVISION = 1;
    public static final String    APP_SUFFIX = "";
    public static final LocalDate APP_DATE = LocalDate.of(2026, 6, 30);

    /**
     * DB_FILE_NAME is the default file name of the SQLite database. It is used
     * when no database path is given on the command line.
     */
    public static final String    DB_FILE_NAME = "timetracker.db";
   
    public static String getVersionString()
    {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        return String.format("%d.%d%s (%s)",
                APP_VERSION, APP_REVISION, APP_SUFFIX, APP_DATE.format(formatter));
    }

}
