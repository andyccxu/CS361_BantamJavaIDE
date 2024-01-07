# Bantam Java IDE

Our Bantam Java IDE consists of a menu bar, a tool bar with three buttons, a code editing box, and a console. 

- **menu bar**: 
  - File menu: open, save, save as, and close files.
  - Edit menu: redo, undo, toggle comments, match brackets, ... 
- **tool bar**:
  - check button: check if the current program is well-formed. If not, display error messages on the console.
  - compile and run: compile and run the Bantam Java program. Redirect standard input/output to the console.
  - stop: stop the program if the program hangs forever.
- **code editing box**: where users edit the Bantam Java source code.
- **console**: it is able to interactively respond to basic shell commands like `cd` and `pwd`. 

![image](https://github.com/andyccxu/CS361_BantamJavaIDE/assets/59164279/751e7605-7de5-417b-87bc-d90b83442982)


## What is Bantam Java

Bantam Java contains a subset of the Java programming language.

A hello world program written in Bantam Java:

```java
class Main {
	void main() {

		var textIO = new TextIO();
		var sys = new Sys();
        
		textIO.putString("Hello World!");
		sys.exit(4);
	}
}
```

Feel free to run the Bantam Java program using our IDE.


## Usage

Please follow the instructions below if you'd like to run the code locally using IntelliJ IDEA.

To create a new Intellij project that uses JavaFX:

0. If you haven't already done so, download the JavaFX library from [here](https://gluonhq.com/products/javafx/). We are using JavaFx version 17.0.9.

1. Create a new Java project (not a JavaFX project) using **JDK17**.  This will cause a new folder to be created for the project. I call this folder "root". If there is not a directory named `src` in the root folder, add one. Put the `proj10PengXuYu` package under the `src` folder.

2. Click on File|Project Structure menu item and, in the Libraries tab of
   the project structure dialog, add the lib directory of the JavaFX library you
   downloaded. In my case, it is located at:
   `/Users/.../BantamJavaIDE/javafx-sdk-17.0.9/lib`

   In Project Structure|Modules, go to the Dependencies tab. Add the richtextfx jar file included in this repo by choosing + and select "JARs or directories".

3. Click on Run|Edit Configurations menu item, and create a new configuration by clicking on the "+" icon in the upper left corner of the configuration dialog.

4. If you don't see an "Add VM Options" text field, click on the "modify options" on the right side of the configurations dialog and select the Add VM Options menu item.  Then, in the "Add VM Options" text field, type in the following text all on one line, except replacing my path inside the quotes with your path to the javafx library:

```
--module-path "your_path_to_javafx_pkg"/javafx-sdk-17.0.2/lib --add-modules javafx.controls,javafx.fxml
```

5. Type in the name of your class with the main method in the appropriate text field in the configurations dialog.  In our case, type `proj10PengXuYu`.

6. Make sure that the Main class is in a folder named `proj10PengXuYu` and make sure that that folder is in the "src" directory in the root folder.

7. Choose Run from the Run menu or click on the green triangle next to your main method in your Main class and your program should compile and run.

From: <https://javabook.bloomu.edu/setupjavafx.html>


## Project Structure

- `ide` package: contains all code files related to the GUI interface of the IDE.
- `bantam` package: contains all code files related to the Bantam Java grammar and the implementation of the lexer, parser, and semantic analyzer.

