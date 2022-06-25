package proj10PengXuYu.ide;

import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;

/**
 * Helper class that contains implementation for handlers of
 * menu items in the File menu. These handler methods are called
 * by the handlers in the Controller, and they actually do the work
 * of handling the events.
 *
 * @author Andy Xu
 */
public class FileMenuController {

    // hashmaps storing last saved contents and saved paths for each tab
    private HashMap<Tab,String> savedContents;
    private HashMap<Tab,String> savedPaths;
    // dialog helper
    private DialogHelper dialogHelper;
    // keep track of the id for new tabs created
    private int newTabID;
    // the TabPane object used by the code editor
    private TabPane tabPane;

    /**
     * Constructor for FileMenuController
     *
     * @param tabPane  the TabPane object passed in by the Controller
     * @param dialogHelper  the DialogHelper object passed in by the Controller
     * */
    public FileMenuController(TabPane tabPane, DialogHelper dialogHelper) {
        this.savedContents = new HashMap<>();
        this.savedPaths = new HashMap<>();
        this.dialogHelper = dialogHelper;
        this.newTabID = 1;
        this.tabPane = tabPane;
    }

    /** Getter for the savedPaths field */
    public HashMap<Tab, String> getSavedPaths() {
        return this.savedPaths;
    }


    /**
     * Handles menu bar item About.
     * Shows a dialog that contains program information.
     */
    public void handleAbout() {
        Dialog<ButtonType> dialog = dialogHelper.getAboutDialog();
        dialog.showAndWait();
    }


    /**
     * Handles menu bar item New. Creates a new tab and adds it to the tabPane.
     */
    public void handleNew() {

        // calls helper method for untitled tabName generation
        String newTabName = "Untitled-" + this.newTabID;
        this.newTabID++;

        // creates tab and sets close behavior
        Tab newTab = new Tab(newTabName);
        newTab.setOnCloseRequest(closeEvent -> {
            // so that correct tab is closed even if not currently selected
            tabPane.getSelectionModel().select(newTab);
            handleClose();
            closeEvent.consume();
        });

        // installs toolTip
        Tooltip tabToolTip = new Tooltip(newTab.getText());
        newTab.setTooltip(tabToolTip);

        // create a code area
        CodeAreaHighlighter highlightedCodeArea = new CodeAreaHighlighter();
        CodeArea codeArea = highlightedCodeArea.getCodeArea();
        newTab.setContent(new VirtualizedScrollPane<>(codeArea));
        // add new tab to the tabPane and sets as topmost
        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().selectLast();
    }


    /**
     * Handles menu bar item Open. Shows a dialog and lets the user select a file to be
     * loaded into the text box.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    public void handleOpen(ActionEvent event) {
        // create a new file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Bantam Java Files", "*.btm"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File selectedFile = fileChooser.showOpenDialog(tabPane.getScene().getWindow());

        // if user selects a file (instead of pressing cancel button
        if (selectedFile != null) {
            try {
                // reads the file content to a String
                String content = new String(Files.readAllBytes(
                        Paths.get(selectedFile.getPath())));
                // open a new tab
                this.handleNew();
                // no need to increment newTabID field
                this.newTabID--;
                // set text/name of the tab to the filename
                Util.getSelectedTab(tabPane).setText(selectedFile.getName());
                Util.getSelectedTextBox(tabPane).replaceText(content);
                // update savedContents field
                this.savedContents.put(Util.getSelectedTab(tabPane), content);
                this.savedPaths.put(Util.getSelectedTab(tabPane), selectedFile.getPath());
                Util.getSelectedTab(tabPane).getTooltip().setText(selectedFile.getPath());
            } catch (IOException e) {
                dialogHelper.getAlert("File Opening Error", e.getMessage()).show();
            }
        }
    }


    /**
     * Handles menu bar item Close. Creates a dialog if the selected tab is unsaved and
     * closes the tab.
     */
    public void handleClose() {
        closeSelectedTab(SaveReason.CLOSING);
    }


    /**
     * Closes the current tab. Prompts the user to save if tab is dirty. If the
     * user chooses to save the changes, the changes are saved and the tab is closed.
     * If the tab is clean or the user chooses to save the dirty tab, the tab is closed.
     *
     * @return Optional the Optional object returned by dialog.showAndWait().
     *                  returns null if tab text is already saved.
     */
    public Optional<ButtonType> closeSelectedTab(SaveReason reason) {
        // If selectedTab is unsaved, opens dialog to ask user whether
        // they would like to save
        if(selectedTabIsDirty()) {
            String fileName = Util.getSelectedTab(tabPane).getText();
            Dialog<ButtonType> saveDialog =
                    dialogHelper.getSavingDialog(fileName, reason);

            Optional<ButtonType> result  = saveDialog.showAndWait();
            // save if user chooses YES
            if (result.isPresent() && result.get() == ButtonType.YES) {
                this.handleSave();
                // Keep the tab if the save is unsuccessful (eg. canceled)
                if (selectedTabIsDirty()) {
                    return result;
                }
            }
            // quit the dialog and keep selected tab if user chooses CANCEL
            else if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                return result;
            }
        }
        // remove tab from tabPane if text is saved or user chooses NO
        this.savedContents.remove(Util.getSelectedTab(tabPane));
        this.savedPaths.remove(Util.getSelectedTab(tabPane));
        tabPane.getTabs().remove(Util.getSelectedTab(tabPane));
        return Optional.empty();
    }


    /**
     * Handler method for menu bar item Exit. When exit item of the menu
     * bar is clicked, the application quits if all tabs in the tabPane are
     * closed properly.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    public void handleExit(ActionEvent event) {
        tabPane.getSelectionModel().selectLast();
        while (tabPane.getTabs().size() > 0) {
            // try close the currently selected tab
            Optional<ButtonType> result = closeSelectedTab(SaveReason.EXITING);
            // if the user chooses Cancel at any time, then the exiting is canceled,
            // and the application stays running.
            if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                return;
            }
        }
        // exit if all tabs are closed
        System.exit(0);
    }


    /**
     * Checks if the text in the selected tab is saved.
     *
     * @return boolean whether the text in the selected tab is dirty (unsaved changes).
     */
    public boolean selectedTabIsDirty() {

        // Gets current contents of tab and its hashed contents (Null if unsaved)
        String currentContents = Util.getSelectedTextBox(tabPane).getText();
        Tab selectedTab = Util.getSelectedTab(tabPane);
        String savedContent = this.savedContents.get(selectedTab);

        // If no saved contents, contents are dirty
        if (savedContent == null) {
            return true;
        }
        // Otherwise, returns false (not dirty) if contents equal,
        // or true if they aren't
        else return ! savedContent.equals(currentContents);

    }

    /**
     * Handler method for menu bar item Save. Behaves like Save as... if the text
     * has never been saved before. Otherwise, save the text to its corresponding
     * text file.
     *
     * @return whether the save was successful
     */
    public boolean handleSave() {
        // if the text has been saved before
        if (savedContents.containsKey(Util.getSelectedTab(tabPane))) {
            // create a File object for the corresponding text file
            File savedFile = new File(savedPaths.get(Util.getSelectedTab(tabPane)));
            try {
                // write the new content to the text file
                FileWriter writer = new FileWriter(savedFile);
                writer.write(Util.getSelectedTextBox(tabPane).getText());
                writer.close();
                // update savedContents field
                savedContents.put(Util.getSelectedTab(tabPane),
                        Util.getSelectedTextBox(tabPane).getText());
                return true;
            } catch (IOException e) {
                dialogHelper.getAlert("File Saving Error", e.getMessage()).show();
                return false;
            }
        }
        // if text in selected tab was not loaded from a file nor ever saved to a file
        else {
            return handleSaveAs();
        }
    }

    /**
     * Handles menu bar item Save as....  a dialog appears in which the user is asked for
     * to save a file with four permitted extensions: .java, .txt, .fxml, and .css.
     *
     * @return whether the save was successful
     */
    public boolean handleSaveAs() {
        // create a new fileChooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Bantam Java Files", "*.btm"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File fileToSave = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        // if user did not choose CANCEL
        if (fileToSave != null) {
            try {
                // save file
                FileWriter fw = new FileWriter(fileToSave);
                fw.write(Util.getSelectedTextBox(tabPane).getText());
                fw.close();
                // update savedContents field and tab text
                this.savedContents.put(Util.getSelectedTab(tabPane),
                        Util.getSelectedTextBox(tabPane).getText());
                this.savedPaths.put(Util.getSelectedTab(tabPane), fileToSave.getPath());
                Util.getSelectedTab(tabPane).setText(fileToSave.getName());
                Util.getSelectedTab(tabPane).getTooltip().setText(fileToSave.getPath());

                return true;
            } catch ( IOException e ) {
                dialogHelper.getAlert("File Saving Error", e.getMessage()).show();

                return false;
            }
        }
        return false;
    }

}
