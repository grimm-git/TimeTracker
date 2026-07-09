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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import TimeTracker.Defaults;

public class BankHolidays
{
    private final ArrayList<LocalDate> listBankHolidays = new ArrayList<>();

    public BankHolidays()
    {
        Path holidayFile = Paths.get(System.getProperty("user.dir"), Defaults.BANK_HOLIDAYS_FILE);
        readBankHolidays(holidayFile);
    }

    /**
     * Reads the bank holidays from the given file. The file is expected to hold
     * one date per line in ISO format (YYYY-MM-DD). Blank lines are skipped and
     * every date is validated as a proper {@link LocalDate}; malformed lines are
     * reported and ignored so a single bad entry does not discard the whole list.
     *
     * @param holidayFile the location of the bank holidays file
     */
    private void readBankHolidays(Path holidayFile)
    {
        if (!Files.exists(holidayFile))
            return;

        try {
            List<String> lines = Files.readAllLines(holidayFile);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty())
                    continue;

                listBankHolidays.add(LocalDate.parse(trimmed));
            }
        } catch (IOException | DateTimeParseException e) {/* IGNORED */}
    }

    public boolean isBankHoliday(LocalDate testDate)
    {
        return listBankHolidays.contains(testDate);
    }
}
