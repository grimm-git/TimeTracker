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
package TimeTracker.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class Language
{
    private Locale locale;
    private ResourceBundle messages;

    public Language()
    {
        this(Locale.getDefault());
    }

    public Language(Locale arg)
    {
        locale = arg;
        messages = ResourceBundle.getBundle("languages.messages", locale);
    }

    public Locale locale()
    {
        return locale;
    }

    public ResourceBundle bundle()
    {
        return messages;
    }

    public String msg(String key, Object... params) {
        String pattern = messages.getString(key);
        if (params.length == 0) {
            return pattern;
        }
        MessageFormat formatter = new MessageFormat(pattern, locale);
        return formatter.format(params);
    }
}
