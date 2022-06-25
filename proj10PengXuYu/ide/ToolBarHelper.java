/*
 * File: ToolBarHelper.java
 * Names: Ricky Peng, Andy Xu, Alex Yu
 * Class: CS 361
 * Project 6
 * Date: March 18
 */

package proj10PengXuYu.ide;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.StyleClassedTextArea;
import proj10PengXuYu.bantam.ast.Program;
import proj10PengXuYu.bantam.parser.Parser;
import proj10PengXuYu.bantam.semant.SemanticAnalyzer;
import proj10PengXuYu.bantam.util.CompilationException;
import proj10PengXuYu.bantam.util.Error;
import proj10PengXuYu.bantam.util.ErrorHandler;
import proj10PengXuYu.bantam.visitor.TranslatorVisitor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Helper class that contains implementation for handlers of
 * buttons in the toolbar.
 *
 * @author Andy Xu
 */
public class ToolBarHelper {

    // get the fileMenuHandlers field of the Controller
    private FileMenuController fileMenuController;
    private TerminalHelper terminalHelper;
    private DialogHelper dialogHelper;

    // the TabPane object used by the code editor
    private TabPane tabPane;
    // the i/o console of the code editor
    private StyleClassedTextArea console;
    // thread used when clicking "Compile&Run" (useful for handleStop method)
    private Thread processThread;
    // Process created when running java
    private Process javaProcess;
    // bind with buttons to disable them when appropriate
    private SimpleBooleanProperty isThreadActive;

    /**
     * Constructor for a ToolBarHelper object.
     *
     * @param fileMenuController  the FileMenuController created in the Controller
     * @param terminalHelper  the TerminalHelper created in the Controller
     * @param dialogHelper  the DialogHelper created in the Controller
     * @param tabPane  the TabPane created in the Controller
     * @param ioConsole  the console created in the Controller
     */
    public ToolBarHelper(FileMenuController fileMenuController,
                         TerminalHelper terminalHelper,
                         DialogHelper dialogHelper,
                         TabPane tabPane,
                         StyleClassedTextArea ioConsole)
    {
        this.fileMenuController = fileMenuController;
        this.terminalHelper = terminalHelper;
        this.dialogHelper = dialogHelper;
        this.tabPane = tabPane;
        this.console = ioConsole;
        this.processThread = null;
        this.javaProcess = null;
        this.isThreadActive = new SimpleBooleanProperty(false);
    }

    /* Getter for isThreadActive field */
    public SimpleBooleanProperty getThreadActive() {
        return this.isThreadActive;
    }


    /**
     * Convert the Bantam Java program to a Java program and compile it.
     * If the tab is dirty, asks user to save. If user chooses to save, the changes are
     * saved and the tab is compiled. If user chooses not to save, the currently saved
     * version of the file is compiled (the unsaved changes are ignored). If the user
     * cancels the dialog, no compilation is performed.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     *
     * @return a boolean that indicates whether the compilation was successful.
     */
    public boolean compileTab(ActionEvent event) {
        // try save the file before compiling it
        if (fileMenuController.selectedTabIsDirty()) {
            // Creates new dialog
            Dialog<ButtonType> saveDialog = dialogHelper.getSavingDialog(
                    Util.getSelectedTab(tabPane).getText(), SaveReason.COMPILING);
            Optional<ButtonType> result = saveDialog.showAndWait();
            // call handleSave() if user chooses YES
            if (result.isPresent() && result.get() == ButtonType.YES) {
                boolean saved = fileMenuController.handleSave();
                if (! saved) {
                    String body = "Compilation was canceled because you " +
                            "aborted saving the file";
                    dialogHelper.getAlert("Compilation Canceled", body,
                            Alert.AlertType.WARNING).show();
                    event.consume();
                    return false;
                }
            }
            // No compilation if user chooses CANCEL
            else if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                event.consume();
                return false;
            }
            else if (result.isPresent() && result.get() == ButtonType.NO) {
                // if user chooses NO and the current tab is not saved before
                if (! fileMenuController.getSavedPaths().containsKey(
                        Util.getSelectedTab(tabPane))) {
                    // make an alert box
                    String body = "Current tab has not been saved. " +
                            "Pleas save before compiling.";
                    dialogHelper.getAlert("Unable to Compile", body).show();
                    event.consume();
                    return false;
                }
            }
        }

        // convert the Bantam Java program to Java program
        String filepath = fileMenuController.getSavedPaths().get(Util.getSelectedTab(tabPane));
        // legal bantam java has to have a Main class with a void main() method
        String filename = filepath.split(
                File.separator)[filepath.split(File.separator).length - 1];
        String newJavaFile = filepath.replace(filename, "Main.java");
        try {
            if (! translateBantamJavaToJava(event, filepath, newJavaFile)) {
                return false;
            }
        } catch (IOException ex) {
            dialogHelper.getAlert("Converting Failed", ex.getMessage()).show();
        }
        // new process builder for compilation
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("javac", newJavaFile);

        // compile the Java program
        try {
            Process process = processBuilder.start();
            // if an error occurs
            if ( process.getErrorStream().read() != -1 ) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));
                String line = reader.readLine();
                console.appendText("\n");
                while (line != null) {
                    console.appendText(line + "\n");
                    line = reader.readLine();
                }
                terminalHelper.displayTerminalPrompt();
            }
            int exitValue = 0;
            try {
                exitValue = process.waitFor();
            } catch (InterruptedException e) {
                dialogHelper.getAlert("Compilation Failed", e.getMessage()).show();
            }
            // if compilation process exits successfully
            if ( exitValue == 0 ) {
                console.appendText("\nCompilation was successful.\n");
                return true;
            }
        }
        catch (IOException ex) {
            dialogHelper.getAlert("Compilation Failed", ex.getMessage()).show();
        }
        return false;
    }

    /**
     * Handler method for Compile & Run button.
     *
     * @param event An ActionEvent object that gives information about the event
     *              and its source.
     */
    public void handleCompileRun(ActionEvent event) {
        // run the program if compilation was successful
        if (this.compileTab(event)) {

            String fullpath = fileMenuController.getSavedPaths().get(
                    Util.getSelectedTab(tabPane)).replace(".btm", "");
            String classname = fullpath.split(
                    File.separator)[fullpath.split(File.separator).length - 1];
            String classpath = fullpath.replace(File.separator + classname, "");

            // new process builder for running with java interpreter
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("java", "-cp", classpath, "Main");
            processBuilder.redirectErrorStream(true);

            // prepare running in a new thread
            processThread = new Thread(() -> {
                try {
                    javaProcess = processBuilder.start();

                    // get outStream and inStream
                    InputStream inStream = javaProcess.getInputStream();
                    OutputStream outStream = javaProcess.getOutputStream();

                    // handles process input
                    console.setOnKeyReleased(new EventHandler<>() {
                        String userInput = "";

                        public void handle(KeyEvent event) {
                            // get the key that is pressed and add it
                            userInput += event.getText();
                            KeyCode keyCode = event.getCode();
                            // if user presses enter
                            if (keyCode == KeyCode.ENTER) {
                                try {
                                    outStream.write(
                                            userInput.getBytes(StandardCharsets.UTF_8));
                                    outStream.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                userInput = "";         // start user input over
                            }
                            // if user hits backspace
                            // ! note that it does not correctly handle the case
                            // ! where user holds backspace key to delete multiple characters
                            if (keyCode == KeyCode.BACK_SPACE) {
                                try {
                                    userInput = userInput.substring(0, userInput.length() - 1);
                                } catch (StringIndexOutOfBoundsException ex) {
                                    userInput = "";
                                }
                            }
                        }
                    });

                    // handles process output
                    int bufferSize = 1024;
                    char[] buffer = new char[bufferSize];

                    Reader reader = new InputStreamReader(
                            inStream, StandardCharsets.UTF_8);
                    for (int numRead;
                         (numRead = reader.read(buffer, 0, buffer.length)) > 0; ) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(buffer, 0, numRead);
                        Util.putOnConsole(sb.toString(), console);
                    }

                    // if process finished successfully
                    Platform.runLater(() -> {
                        console.appendText(String.format(
                                "\nProcess finished with exit code %d.\n",
                                javaProcess.exitValue()));
                        terminalHelper.displayTerminalPrompt();
                    });
                }
                catch (IOException ex) {
                    Platform.runLater(() -> {
                        dialogHelper.getAlert("Runtime Error", ex.getMessage()).show();
                        terminalHelper.displayTerminalPrompt();
                    });
                }
                // after the thread is done running, it should set the internal
                // field back to null so that the bindings can recognize that
                // there is no process running
                this.processThread = null;
                this.isThreadActive.set(false);
                // set back to the default handler for terminal input
                terminalHelper.setTerminalHandler();
            });
            // set the boolean property field and start the thread
            this.isThreadActive.set(true);
            processThread.start();
        }
    }

    /**
     * Handles the stop button. Forcefully stops the thread and resets the processThread
     * to null.
     * @deprecated
     */
    public void handleStop() throws IOException {
        // TODO stop the process as well
        if (processThread != null) {
            javaProcess.destroyForcibly();
            processThread.stop(); // ! deprecated
            this.isThreadActive.set(false);
            processThread = null;
            Util.putOnConsole("\n\n   Process manually stopped.\n", console);
            terminalHelper.displayTerminalPrompt();
            terminalHelper.setTerminalHandler();
        }
    }


    /**
     * Handles the Check button in the toolbar. Saves the selected code area first.
     * Then Scanner, Parser, and SemanticAnalyzer are called to check whether
     * the program contains any errors. If so, those errors are printed
     * in the console area. If there are no errors, then a message to that
     * effect is printed in the console.
     */
    public void handleCheck() {
        // save the selected code area first
        if (! fileMenuController.handleSave()) {
            dialogHelper.getAlert("Checking Stopped",
                    "You canceled file saving.", Alert.AlertType.WARNING).show();
            return;
        }
        // new error handler, parser, and semantic analyzer
        ErrorHandler errorHandler = new ErrorHandler();
        Parser parser = new Parser(errorHandler);
        SemanticAnalyzer analyzer = new SemanticAnalyzer(errorHandler);

        // filepath
        String inFile = fileMenuController.getSavedPaths().get(
                Util.getSelectedTab(tabPane));
        try {
            Util.putOnConsole("\n========== Results for checking "
                    + inFile + " =============\n", console);
            try {
                Program program = parser.parse(inFile);
                analyzer.analyze(program);
                Util.putOnConsole("  Checking was successful.\n", console);
            } catch (CompilationException ex) {
                if (ex.getMessage() != null) {
                    Util.putOnConsole(ex.getMessage() + "\n", console);
                }
                Util.putOnConsole("  There were errors:\n", console);
                List<Error> errors = errorHandler.getErrorList();
                for (Error error : errors) {
                    Util.putOnConsole("\t" + error.toString() + "\n", console);
                }
            }
        }
        catch (IOException ex) {
            dialogHelper.getAlert("IO Exception",
                    "Unable to display messages to the console.");
        }
        terminalHelper.displayTerminalPrompt();
    }


    /**
     * Convert the Bantam Java program to a Java program and store the converted
     * Java program in a new file.
     *
     * @param event  An ActionEvent object that gives information about the event
     *              and its source.
     * @param bantamFile  the file path of the existing bantam java program
     * @param newFilePath  the file path of the newly created java program
     *
     * @return boolean  whether the translation was successful
     */
    private boolean translateBantamJavaToJava(ActionEvent event,
                                              String bantamFile,
                                              String newFilePath) throws IOException {
        // check if the Bantam Java program is legal
        ErrorHandler errorHandler = new ErrorHandler();
        Parser parser = new Parser(errorHandler);
        SemanticAnalyzer analyzer = new SemanticAnalyzer(errorHandler);
        TranslatorVisitor translator = new TranslatorVisitor();
        try {
            Program program = parser.parse(bantamFile);
            analyzer.analyze(program);
            // get the translated code
            String javaCode = translator.generateOutputString(program);

            // create new file to store the translated code
            File newFile = new File(newFilePath);
            newFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
            writer.write(javaCode);
            writer.close();
            return true;
        } catch (CompilationException ex) {
            dialogHelper.getAlert("Illegal Bantam Java Program",
                    "Use Check button to see the errors in the program.").show();
            event.consume();
            return false;
        }
    }

}
