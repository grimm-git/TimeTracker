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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

public class Session
{
    private LocalDateTime SessionStart;
    private LocalDateTime SessionEnd;

    public Session()
    {
        SessionStart = LocalDateTime.now();
        SessionEnd = LocalDateTime.MIN;
    }

    /**
     * Reconstructs a Session from stored values, e.g. when read back from the
     * database. A not yet finished session is expected to use LocalDateTime.MIN
     * as its end time.
     *
     * @param start the start time of the session
     * @param end   the end time of the session, or LocalDateTime.MIN if the
     *              session has not been finished yet
     */
    public Session(LocalDateTime start, LocalDateTime end)
    {
        SessionStart = start;
        SessionEnd = end;
    }

    /**
     * Check, if the session has a valid end date
     * 
     * @return TRUE, if the session is still open
     */
    public boolean isSessonFinished()
    {
        return SessionEnd != LocalDateTime.MIN;
    }

    public void finishSession()
    {
        SessionEnd = LocalDateTime.now();
    }

    public LocalDateTime getSessionStart()
    {
        return SessionStart;
    }

    public LocalDateTime getSessionEnd()
    {
        return SessionEnd;
    }

    /**
     * Returns the day of week of the session start date as a short English
     * name, e.g. "Mon", "Tue", "Wed".
     *
     * @return the abbreviated English name of the start day
     */
    public String getDayName()
    {
        return SessionStart.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    /**
     * Returns the elapsed time between session start and session end as a
     * Duration object.
     *
     * @return the session duration
     */
    public Duration getWorkTime()
    {
        return Duration.between(SessionStart, SessionEnd);
    }
}
