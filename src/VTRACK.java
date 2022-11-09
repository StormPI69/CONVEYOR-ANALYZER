import java.awt.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Expected input format: java VTRACK <command> <target>
 * 
 * Available Commands - a : Reads in a gif and analyzes the content before
 * returning a report on the number of cylinders present. java VTRACK a <Folder
 * name> <outputfile> - h : Accesses the archive of former results and displays
 * the report. java VTRACK h
 */
public class VTRACK {

    public static void main(String[] args) {

        // fileHandler fileHandler = new fileHandler();
        if (args[0].contains("a")) {
            JFileChooser f = new JFileChooser();
            f.setCurrentDirectory(new java.io.File("."));
            f.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            f.showOpenDialog(null);
            System.out.println(f.getSelectedFile().toPath());
            System.out.println("Processing data from: " + f.getSelectedFile().toPath());
            ArrayList<BufferedImage> listOfImages = fileHandler.OpenGIF(f.getSelectedFile().getAbsolutePath().toString());
            Analyzer Ana = new Analyzer(listOfImages, f.getSelectedFile().getName());
            Ana.runAnalyzer();
            
        } else if (args[0].contains("h")) {
            menu.start();

        }
    }
}