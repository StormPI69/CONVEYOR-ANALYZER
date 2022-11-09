import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.*;
/**
 * This class is for reading in GIF images into the program and saving them to a list that will store the images.
 * The class returns the list of images to the Analyser class that is responsible for correlating the image manipulation.
 *
 * @author Peter Mhlanga, Lucas Brooke, Storm Hendricks.
 *
        */
public class fileHandler {
    //Creating arraylist that stores the images.
    // static ArrayList<BufferedImage> listOfImages;

    /**
     * Constructs and initializes the listOfImages that will store the images.
     * listOfImages is the list that will store the images.
     */
   public fileHandler(){
       //initialises the list.
       ArrayList listOfImages=new ArrayList<BufferedImage>();
   }

    /**
     * This method reads in a set of GIF images from an input directory and stores them in a given output directory.
     *
     * @param inputPathname The input pathname.
     * @return A list that contains the GIF images from the input directory.
     */
    public static ArrayList<BufferedImage> OpenGIF(String inputPathname) {
        
        ArrayList listOfImages = new ArrayList<BufferedImage>();
        // File representing the folder that you select using a FileChooser.
        File dir = new File(inputPathname);
        // array of supported extensions
        // (In the prac spec its gif but I added other  formats just incase
        // we are given a different extension we can add here).
        String[] EXTENSIONS = new String[]{
                "gif", "png", "bmp" // and other formats you need
        };
        // filter to only identify images that are GIF format extension.
        FilenameFilter IMAGE_FILTER = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                for (final String ext : EXTENSIONS) {
                    if (name.endsWith("." + "gif")) {
                        return (true);
                    }
                }
                return (false);
            }
        };
        //To printout image specs:
        if (dir.isDirectory()) { // make sure it's a directory
            for ( final File f : dir.listFiles(IMAGE_FILTER)) {
                BufferedImage img = null;
                try {
                    img = ImageIO.read(f); //read in images usion ImageIO
                    listOfImages.add(img); //save images to list.

                } catch (final IOException e) {
                    // handle errors here
                    System.out.println("Error! Could not read in images!");
                }
            }
        }
        return listOfImages;
    }

}


