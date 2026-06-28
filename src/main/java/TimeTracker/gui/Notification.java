/*
 * Copyright (C) 2017 Matthias Grimm
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

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

/**
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
            _alert(AlertType.INFORMATION, "Information", msg);
        else
            Platform.runLater(() -> {
                _alert(AlertType.INFORMATION, "Information", msg);
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
            _alert(AlertType.WARNING, "Warning", msg);
        else
            Platform.runLater(() -> {
                _alert(AlertType.WARNING, "Warning", msg);
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
            _alert(AlertType.ERROR, "Error", msg);
        else
            Platform.runLater(() -> {
                _alert(AlertType.ERROR, "Error", msg);
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
        alert.setTitle("Please confirm");
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
        alert.setTitle("Please confirm");
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
}
