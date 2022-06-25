# CS361_BantamJavaIDE
The source code for the final project of CS361 Object-Oriented Programming in Colby College. The project is cumulative, and we built a Bantam Java IDE working in teams.

## Key Features
<img width="800" alt="image" src="https://user-images.githubusercontent.com/59164279/175782714-2c1c3709-1ae9-4b20-939e-ec6a024cb780.png">

Our Bantam Java IDE consists of a menu bar, a tool bar with three buttons, a text box, and a console at the bottom. It is able to open and edit Bantam Java program files, and save the progress to the local directory. The console is able to interactively respond to basic bash commands like `cd` and `pwd`. By using the buttons in the tool bar, we can check if the current program is well-formed and semantically correct, and we can also compile and run the program. The output will be displayed in the console. 

## How to Run
We recommend using Java JDK version 10 on a powerful environment like IntelliJ Idea to run this project. Using Java JDK 10 does not require additionally download and install the JavaFX GUI package.

On IntelliJ, open the directory that contains the source code files as a new project. In the project structure settings, set the right java SDK version and language level (we recommend version 10). Also, in the Modules page, add the richtextfx jar file to the dependencies list. Lastly, edit the run configurations by choosing the Main class and adding a VM option. The VM option should be like `--module-path <path-to-javafx>/javafx-sdk-10.0.2/lib --add-modules javafx.controls,javafx.fxml`.

## Project Structure

- `ide` package: contains all code files related to the GUI interface of the IDE.
- `bantam` package: contains all code files related to the Bantam Java grammar and the implementation of the lexer, parser, and semantic analyzer.

