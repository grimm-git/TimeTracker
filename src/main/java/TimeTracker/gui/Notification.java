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
package TimeTracker.gui;

import java.util.Optional;

import TimeTracker.Registry;
import TimeTracker.util.Language;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

/**
 * Class provides convinient notification windows for various ocasions
 * It fully supports I18N via ResourceBundles
 * 
 * @author Matthias Grimm <matthiasgrimm@users.sourceforge.net>
 */
public class Notification
{
    /**
     * Notification window to display an information message
     * 
     * @param msg  Message text to display
     */
    public static void showInfo(String msg)
    {
        if (Platform.isFxApplicationThread())
            _alert(AlertType.INFORMATION, i18n("notification.info"), msg);
        else
            Platform.runLater(() -> {
                _alert(AlertType.INFORMATION, i18n("notification.info"), msg);
            });
    }

    /**
     * Notification window to display a warning message
     * 
     * @param msg  Message text to display
     */
    public static void showWarning(String msg)
    {
        if (Platform.isFxApplicationThread())
            _alert(AlertType.WARNING, i18n("notification.warn"), msg);
        else
            Platform.runLater(() -> {
                _alert(AlertType.WARNING, i18n("notification.warn"), msg);
            });
    }

    /**
     * Notification window to display an error message
     * 
     * @param msg  Message text to display
     */
    public static void showError(String msg)
    {
        if (Platform.isFxApplicationThread())
            _alert(AlertType.ERROR, i18n("notification.error"), msg);
        else
            Platform.runLater(() -> {
                _alert(AlertType.ERROR, i18n("notification.error"), msg);
            });
    }
    
    /**
     * This dialog asks the user for confirmation
     * 
     * @param title  Question to ask the user
     * @param msg    Explanation for this confirmation request
     * @return  True, if the user agrees, false otherwise
     */
    public static boolean getConfirmation(String title, String msg)
    {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(i18n("notification.confirm"));
        alert.setHeaderText(title);
        alert.setContentText(msg);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        Optional<ButtonType> result = alert.showAndWait();
        
        return result.get() == ButtonType.OK;
    }

    /**
     * This dialog asks the user for a decission
     * 
     * @param title  Question to ask the user
     * @param msg    Explanation for this confirmation request
     * @param optionA  Text for option A button
     * @param optionB  Text for option B button
     * @return  0, if the user chooose option A and 1 if he choose option B
     */
    public static int getDecission(String title, String msg, String optionA, String optionB)
    {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(i18n("notification.confirm"));
        alert.setHeaderText(title);
        alert.setContentText(msg);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        ButtonType buttonA = new ButtonType(optionA);
        ButtonType buttonB = new ButtonType(optionB);
        alert.getButtonTypes().setAll(buttonA, buttonB);

        Optional<ButtonType> result = alert.showAndWait();
        
        return (result.get() == buttonA) ? 0 : 1;
    }
    
    private static void _alert(AlertType type, String title, String msg)
    {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
     
    /**
     * Convinience wrapper for Language.msg()
     * 
     * @param key     translated text to request
     * @param params  parameter list to be inserted in the translated text into {0} placeholders
     * @return text for the set LOCALE
     */
    private static String i18n(String key, Object ...params)
    {
        Registry Reg = Registry.get();
        Language I18N = Reg.getI18N();
        return I18N.msg(key, params);
    }
}
