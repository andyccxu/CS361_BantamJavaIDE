/*
 * File: TerminalHelper.java
 * Names: Ricky Peng, Andy Xu, Alex Yu
 * Class: CS 361
 * Project 6
 * Date: March 18
 */

package proj10PengXuYu.ide;

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;


/**
 * Helper class that handles terminal input and executes the command.
 *
 * @author Andy Xu
 */
public class TerminalHelper {

    private ProcessBuilder processBuilder;
    private DialogHelper dialogHelper;
    // keeps track of current working directory (useful for cd command)
    private String currentWorkingDir;
    // the console object of the IDE
    private StyleClassedTextArea console;

    /**
     * Constructor for TerminalHelper
     *
     * @param dialogHelper  the DialogHelper object passed in by the Controller
     * @param ioConsole  the console passed in by the Controller
     */
    public TerminalHelper(DialogHelper dialogHelper, StyleClassedTextArea ioConsole) {
        this.processBuilder = null;
        this.dialogHelper = dialogHelper;
        this.currentWorkingDir = System.getProperty("user.dir");
        this.console = ioConsole;
    }

    /**
     * Sets the default event handler for key released event on terminal.
     * By default, when no java process is running, the terminal waits for
     * user input, which is assumed to be bash commands.
     */
    public void setTerminalHandler() {
        console.setOnKeyReleased(new EventHandler<>() {
            String userInput = "";
            public void handle(KeyEvent event) {
                // get the key that is pressed and add it
                userInput += event.getText();
                KeyCode keyCode = event.getCode();
                // if user presses enter
                if (keyCode == KeyCode.ENTER) {
                    userInput = userInput.trim();
                    if (! userInput.isEmpty()) {
                        execute(userInput);
                        userInput = "";         // start user input over
                    }
                    else {
                        displayTerminalPrompt();
                    }
                }
                // handles situation where user deletes their input
                if (keyCode == KeyCode.BACK_SPACE) {
                    String[] lines = console.getText().split("\n");
                    // get the current line
                    String lastLine = lines[lines.length - 1];
                    // get appropriate user input after hitting backspace
                    userInput = lastLine.substring(lastLine.indexOf("$") + 1);
                }
            }
        });
    }

    /**
     * Displays the prompt on terminal. Each new line on the terminal should
     * be started with this prompt and waits for user input commands.
     */
    public void displayTerminalPrompt() {
        // try reading the machine name. eg. Andy's MacBook Pro
        String machineName;
        try {
            machineName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            machineName = "Unknown Machine";
        }

        // read the directory name of the current working directory
        String workingDirName = currentWorkingDir.split(File.separator)[
                currentWorkingDir.split(File.separator).length - 1];

        // integrates all to the prompt string
        String prompt = machineName + ":" + workingDirName + "  "
                + System.getProperty("user.name") + "$ ";
        try {
            Util.putOnConsole(prompt, console);
        } catch (IOException e) {
            dialogHelper.getAlert("Fails to Write on Console", e.getMessage()).show();
        }
    }

    /**
     * Executes the command user typed to the terminal, and displays the
     * output.
     *
     * @param command   the command user typed to the terminal.
     */
    public void execute(String command) {

        this.processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(currentWorkingDir));
        // assumes to be a bash command
        // TODO: make it works on Windows machine using if statement
        processBuilder.command("sh", "-c", command);
        processBuilder.redirectErrorStream(true);

        // handles cd command by a helper method
        if (command.split("[ ]+")[0].equals("cd")) {
            this.executeCD(command);
            return;
        }

        // else, create a new thread to run the bash command
        Thread thread = new Thread(() -> {
            try {
                Process process = processBuilder.start();
                OutputStream outStream = process.getOutputStream();
                InputStream inStream = process.getInputStream();

                // handles process output
                Util.putOnConsole(convertStreamToString(inStream), console);

                // give a time limit for the command to finish
                boolean isFinished = process.waitFor(30, TimeUnit.SECONDS);
                outStream.flush();
                outStream.close();

                if (! isFinished) {
                    process.destroyForcibly();
                }

                displayTerminalPrompt();

            } catch ( IOException | InterruptedException ex) {
                dialogHelper.getAlert("Error on Running Command", ex.getMessage()).show();
            }
        });
        thread.start();
    }

    /**
     * Helper method that specifically handles the cd command.
     *
     * @param command   the command user typed to the terminal.
     */
    private void executeCD(String command) {
        // if cd is passed in without further arguments
        if (command.equals("cd")) {
            String homeDir = System.getProperty("user.home");
            processBuilder.directory(new File(homeDir));
            currentWorkingDir = homeDir;
            displayTerminalPrompt();
            return;
        }
        // if the length of cd command is not right
        if (command.split("[ ]+").length != 2) {
            try {
                Util.putOnConsole("Usage: cd [path]\n", console);
            } catch (IOException e) {
                dialogHelper.getAlert("Fails to Write on Console",
                        e.getMessage()).show();
            }
            displayTerminalPrompt();
            return;
        }
        // else the command has the right length
        String path = command.split("[ ]+")[1];
        // check if the directory exists before set it as working directory
        if (path.split("/").length == 1) {
            path = currentWorkingDir + "/" + path;
        }
        if (Files.exists(Paths.get(path))) {
            processBuilder.directory(new File(path));
            currentWorkingDir = path;
        }
        else {
            // prints error message on terminal
            try {
                Util.putOnConsole("cd:  " + path
                        + " : No such file or directory\n", console);
            } catch (IOException e) {
                dialogHelper.getAlert("Fails to Write on Console",
                        e.getMessage()).show();
            }
        }
        displayTerminalPrompt();
    }

    /**
     * Helper method that converts InputStream to a String.
     *
     * @param inStream  the InputStream to be read.
     * @return  the String read from the InputStream
     */
    private String convertStreamToString(InputStream inStream) throws IOException {
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];

        Reader reader = new InputStreamReader(inStream, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (int numRead; (numRead = reader.read(buffer, 0, buffer.length)) > 0; ) {
            sb.append(buffer, 0, numRead);
        }
        return sb.toString();
    }


}
