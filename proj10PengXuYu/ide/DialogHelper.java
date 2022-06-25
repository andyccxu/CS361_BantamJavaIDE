/*
 * File: Controller.java
 * Names: Ricky Peng, Andy Xu, Alex Yu
 * Class: CS 361
 * Project 6
 * Date: March 18
 */

package proj10PengXuYu.ide;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;


/**
 * Helper class that constructs alert boxes and dialogs.
 * Also customizes string to display for different scenarios.
 *
 * @author Caleb Bitting
 */
public class DialogHelper {

    /**
     * Dialog producer for handleAbout() method only. Returns a
     * dialog that contains necessary information about the app.
     *
     * @return a Dialog object that displays the About information.
     */
    public Dialog<ButtonType> getAboutDialog() {
        // create a new dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setContentText("This is a Bantam Java IDE! \n\n "
                + "Authors: Ricky Peng, Alex Yu, and Andy Xu");
        // add a close button so that dialog closing rule is fulfilled
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        return dialog;
    }

    /**
     * Helper function that takes a user-defined title and
     * body content and returns an alert box that contains the information.
     *
     * @param title a String that indicates the title of alert box.
     * @param alertBody a String that indicates the body of alert box
     *
     * @return an AlertBox of type Error
     */
    public Alert getAlert(String title, String alertBody) {
        // By default, an error alert
        return getAlert(title, alertBody, Alert.AlertType.ERROR);
    }

    /**
     * Overrides the above method by adding a parameter that specifies
     * the alert type.
     * Helper function that takes a user-defined title and
     * body content and returns an alert box that contains the information.
     *
     * @param title a String that indicates the title of alert box.
     * @param alertBody a String that indicates the body of alert box
     * @param alertType an AlertType object that specifies the type of alert.
     *
     * @return a customized AlertBox
     */
    public Alert getAlert(String title, String alertBody, Alert.AlertType alertType) {
        Alert alertBox = new Alert(alertType);
        alertBox.setHeaderText(title);
        alertBox.setContentText(alertBody);

        return alertBox;
    }

    /**
     * Returns the Dialog object to be shown to handle user's unexpected behavior on
     * file saving.
     *
     * @param fileName  a string of filename to customize the prompt in display.
     * @param reason    an Enum object that indicates the reason for asking to save
     *                  file. Can either be Exiting, Closing, or Compiling.
     *
     * @return a Dialog asking if user wants to save the file before next step.
     */
    public Dialog<ButtonType> getSavingDialog(String fileName, SaveReason reason) {
        Dialog<ButtonType> dialog = new Dialog<>();

        String promptText;
        switch (reason) {
            case CLOSING:
                promptText = String.format("Do you want to save %s before closing it?",
                                            fileName);
                break;

            case EXITING:
                promptText = String.format("Do you want to save %s before exiting?",
                                            fileName);
                break;

            case COMPILING:
                promptText = String.format("Do you want to save %s before compiling?",
                                            fileName);
                break;

            default:
                promptText = "How did we get here?";
                break;
        }

        dialog.setContentText(promptText);

        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

        return dialog;
    }

}
