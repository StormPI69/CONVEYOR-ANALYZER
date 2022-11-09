
import java.util.ArrayList;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Class to analyze image feed and count objects that pass through.
 * Initialized with a BufferedImage list and directories for input and output.
 * Analyzer performs analysis on the feed. Data being processed is stored in
 * arraylists of type objects
 *
 * @author Lucas, Storm and Peter
 * @version 0.0.4
 */
public class Analyzer {
    int currentImageIndex;
    int offSetFromEnd = 10;
    BufferedImage img;
    BufferedImage rep;
    int acceptAbleDelta = 180;
    ArrayList<BufferedImage> listOfImages;
    ArrayList<objects> cylinders;
    ArrayList<objects> miscObjects;
    ArrayList<objects> incompleteObjects;
    String directory;
    String outputFile="Output/Output.txt";

    /**
     * Analyzer class constructor. To run the Analyzer please call runAnalyzer
     *
     * @param listOfImages Ordered Arraylist of Buffered Images that combined form the video feed.
     * @param directory String representation of relative path to the source directory for the
     * provided list of images.
     */
    public Analyzer(ArrayList<BufferedImage> listOfImages, String directory){
        this.listOfImages = listOfImages;
        this.directory = directory;
        //System.out.println(directory);
        

        incompleteObjects = new ArrayList<objects>();
        cylinders = new ArrayList<objects>();
        miscObjects = new ArrayList<objects>();

        // Prep variables to handle output of new image with highlighted objects.
        img =listOfImages.get(0);//listOfImages.size()-1);
        rep = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rep.createGraphics();
        g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), null);
        g.dispose();
    }

    /**
     * Method to run the Analyzer class. Called after constructing the Analyzer object, runAnalyzer
     * handles the analysis of the gif and sends the results to be compiled by the dataHandler.
     */
    public void runAnalyzer(){
        try{
            // scan first image
            int xCoord = 0;
            scanImage();

            // scans subsequent frames.
            for (int i=2; i < listOfImages.size(); i++){
                if (listOfImages.size()>0){xCoord = listOfImages.get(i).getTileWidth()-offSetFromEnd-1;}
                // iterate over object feed.
                iterateObjects();
                scancolumn(i, xCoord);
            }

            // Scans the last columns of pixels from the last frame, these would otherwise not be scanned due to the offSetFromEnd changing which
            // Column is scanned in an image.
            int lastImageIndex = listOfImages.size()-1;
            for (int i=listOfImages.get(lastImageIndex).getTileWidth()-offSetFromEnd; i < listOfImages.get(lastImageIndex).getTileWidth(); i++){
                scancolumn(lastImageIndex, i);
            }
            
            flushIncompleteObjects();
            writeResults();

        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Image Index: " + currentImageIndex + "\nIncomplete Objects: "+incompleteObjects.size());
        }
    }

    /**
     Scan entire image to check for existing objects on the convyor belt.
     Intended for single use at the start of video feed analysis.
     Always acts on listofimages[0]
     */
    private void scanImage(){
        // For loop across the x Coords of index 1 from listOfImages. Calling scancolumn(0, xCoord)
        for (int i=0; i < listOfImages.get(1).getTileWidth()-offSetFromEnd; i++){
            scancolumn(1, i);
        }
    }

    /**
     * Scans column of new pixels. Checks if there exists objects on each x plane.
     *
     * @param imageIndex The index from listOfImages of the image being processed currently.
     * @param xCoord The x coordinate of the column to be scanned.
     */
    private void scancolumn(int imageIndex, int xCoord){
        currentImageIndex = imageIndex;
        int[] coords = new int[]{xCoord, 0}; // Expected as (x, y) This is the pixel the scan is centered around.
        int[] compTile = new int[]{0, 0}; // Generated as a coordinate relative to coords, this is the 
        // coordinate the algorithm uses to make decisions about coords.
        int nextYmax = getNextYmax(0); // Tracks the next y value at which the algorithm checks if it can classify an object as a disk or mist object.
        
        // Last object pixel is used to inform the algorithm the last time it dealt with a pixel that was associated to an object,
        // This combined with information provided by yMin and yMax allows for a constant time complexity check when checking if two objects need
        // to be joined or if an object was not detected on the latest pass and therefore can be deemed to be fully read in, thus allowing it to be processed.
        int lastObjectPixel = -1;

        // For Loop going down column of pixels.
        for (int i=0; i < listOfImages.get(imageIndex).getTileHeight(); i++){
            coords[1] = i;
            // Check pixel is not background
            if (!comparePixeltoInt(coords, -16777216, imageIndex, acceptAbleDelta)){
                // Comparison to pixel to the left
                if (coords[0]-1>=0){
                    compTile = new int[]{coords[0]-1, coords[1]};
                    if(!comparePixeltoInt(compTile, -16777216, imageIndex, acceptAbleDelta)){
                        link(coords, compTile);
                        lastObjectPixel = i;
                    }
                }
                // Comparison to Pixel above
                if (coords[1]-1>=0){
                    compTile = new int[]{coords[0], coords[1]-1};
                    if(!comparePixeltoInt(compTile, -16777216, imageIndex, acceptAbleDelta)){
                        // If lastObjectPixel==i this implies that the pixel to the left of coords was part of an object.
                        // Hence coords is part of an object.  By the line of code above we know that the pixel above is also
                        // Part of an object. Hence the two objects may need to be combined now that coords links them.
                        if (lastObjectPixel==i){
                            int left = matchObject(new int[]{coords[0]-1,coords[1]});
                            int top = matchObject(compTile);
                            // Checks the objects are not already combined.
                            if (left != top){
                                if (incompleteObjects.get(left).joinObject(incompleteObjects.get(top))){
                                    // Avoids duplication of points formerly in the top object
                                    incompleteObjects.remove(top);
                                }
                            }
                        }
                        // Else statement is for the case that the pixel to the left was not part of an object, 
                        // hence the algorithm merely links coords to the above pixel's object and does nothing more.
                        else {
                            // System.out.println("Scan Column Calling link at line 126 on imageIndex: " + imageIndex);
                            link(coords, compTile);
                            lastObjectPixel = i;
                        }
                    }
                }
                // No object found to link to, creates a new object.
                if (lastObjectPixel!=i){
                    // System.out.println("Scan Column creating new object on imageIndex: " + imageIndex);
                    objects latest = new objects(coords, imageIndex);
                    incompleteObjects.add(latest);
                }
            }
            // Checks if an object is complete, and sorts complete objects.
            if (i == nextYmax){
                compTile[1] = i;
                markObjectComplete(lastObjectPixel, xCoord, compTile, imageIndex);
                nextYmax = getNextYmax(i);
            }
        }
        errorCheck();
    }

    /**
     * Finds and marks the recently passed object complete.
     * @param lastObjectPixel The y coordinate of the last pixel that registered as an object.
     * @param xCoord The x coordinate the scan is being run on.
     * @param compTile The current coordinate of the scan.
     * @param delta The discrepancy between black and the current pixel permitted before a pixel is deemed to be part of an object.
     */
    private void markObjectComplete(int lastObjectPixel, int xCoord, int[] compTile, int imageIndex){
        compTile = findObjectPixel(xCoord, compTile, imageIndex, acceptAbleDelta);
        int objectA = matchObject(compTile);
        if (objectA>=0 && incompleteObjects.get(objectA).yMin > lastObjectPixel){
            if (incompleteObjects.get(objectA).checkCylinder(listOfImages.get(imageIndex))){
                if (incompleteObjects.get(objectA).getSize()>0){cylinders.add(incompleteObjects.get(objectA));}
            }
            else{
                if (incompleteObjects.get(objectA).getKeypoints().size()>1){
                    miscObjects.add(incompleteObjects.get(objectA));
                }
            }
            // Removes now completed object from incomplete objects.
            incompleteObjects.remove(objectA);
        }
    }

    /**
     * Iterates back over the image loopking for an object pixel.
     * @param xCoord The starting x coordinate to search back from.
     * @param compTile the point to be checked first.
     * @param imageIndex index of the image that all coordinates in the method refer to.
     * @param delta The discrepancy between black and the current pixel permitted before a pixel is deemed to be part of an object.
     * @return The coordinate of the object located on the same y as compTile.
     */
    private int[] findObjectPixel(int xCoord, int[] compTile, int imageIndex, int delta){
        boolean pixelNotFound = true;
        for (int n=xCoord; n>=0; n--){
            compTile[0] = n;
            if(!comparePixeltoInt(compTile, -16777216, imageIndex, delta)){
                pixelNotFound = false;
                break;
            }
        }
        if (pixelNotFound==true){compTile[0] = -1;}
        return compTile;
    }

    /**
     * Given a coordinate of an object pixel, matches the pixel to an object that is currently being held in
     * incompleteObjects.
     *
     * @param coord Coordinate of an object pixel to be matched to an Object stored in incompleteObjects.
     * @return The index from incompleteObjects of the object that contains coord.
     */
    private int matchObject(int[] coord){
        // System.out.println("matchObject Called on: (" + coord[0] + ", " + coord[1] + "), Image Index: " + currentImageIndex);
        for (int i=0; i< incompleteObjects.size(); i++){
            // System.out.println("i: " + i + " incompleteObjects.get(i).yMin:" + incompleteObjects.get(i).yMin +" <= "+coord[1]+" and incompleteObjects.get(i).yMax: "+ incompleteObjects.get(i).yMax +  ">= " +coord[1]);
            if (incompleteObjects.get(i).yMin <= coord[1] && incompleteObjects.get(i).yMax >= coord[1]){

                if (incompleteObjects.get(i).compare(coord)){
                    // System.out.println("Returning i: "+i);
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Compares pixel to an RGB value
     *
     * @param pixelValues First pixel to be compared. Expected as [x,y]
     * @param blackValue RGB value to be compared to.
     * @param imageIndex The index from listOfImages of the image being processed currently.
     * @return Boolean thats true if pixel colour matches to withing a small margin of error.
     */
    private boolean comparePixeltoInt(int[] pixelValues, int blackValue, int imageIndex,double delta){
        int blackRGB=blackValue; //
        int pixelRGB=listOfImages.get(imageIndex).getRGB(pixelValues[0], pixelValues[1]);

        int blackRed = (blackRGB>>16) & 0xff;
        int pixelRed = (pixelRGB>>16) & 0xff;

        int blackGreen =(blackRGB>>8) & 0xff;
        int pixelGreen = (pixelRGB>>8) & 0xff;

        int blackBlue = (blackRGB) & 0xff;
        int pixelBlue = (pixelRGB) & 0xff;

        int deltaRed = ABS( blackRed-  pixelRed);
        int deltaGreen = ABS( blackGreen - pixelGreen);
        int deltaBlue = ABS(blackBlue - pixelBlue);

        if ((deltaBlue+deltaGreen+deltaRed)<acceptAbleDelta){
            return true;
        }
        return false;
    }




    /**
     * Adds compTile to the object that coords matches to.
     *
     * @param coords coordinate of existing object.
     * @param compTile coordinate of new pixel to be joined to coords object.
     */
    private void link(int[] compTile, int[] coords){
        // System.out.println("Coords to Link: (" + coords[0] + ", " + coords[1] + ") and (" +compTile[0] + ", " +compTile[1] + ")");
        int index = matchObject(coords);
        if(index==-1){
            // System.out.println("Enter Repair Operation, Link line 292, Image Index: " + currentImageIndex + ", ("+coords[0]+", "+coords[1]+")");
            objects latest = new objects(coords, currentImageIndex);
            incompleteObjects.add(latest);
            index = matchObject(coords);
            
            // int[] test = new int[]{coords[0], coords[1]-1};
            int[] test = new int[]{coords[0]-1, coords[1]};
            if (testAndAddPixel(test, index, currentImageIndex, acceptAbleDelta)){
                index = matchObject(coords);
            }
            // test = new int[]{coords[0]-1, coords[1]};
            test = new int[]{coords[0], coords[1]-1};
            if (testAndAddPixel(test, index, currentImageIndex, acceptAbleDelta)){
                index = matchObject(coords);
            }
        }
        incompleteObjects.get(index).addKeypoint(compTile);
    }

    /**
     * Tests if a pixel is an object pixel and if so adds it (or its related object) to the object stored at index.
     * @param test A coordinate point to be tested.
     * @param index index of the object the pixel would be joined to if it were an object pixel.
     * @param imageIndex the index in listOfImages that is referenced by all coordinates in this method.
     * Acceptable delta, the value difference from black that determines if a pixel qualifies as an object pixel.
     */
    private boolean testAndAddPixel(int[] test, int index, int imageIndex, int acceptAbleDelta){
        if (!comparePixeltoInt(test, -16777216, imageIndex, acceptAbleDelta)){
            int testIndex = matchObject(test);

           
            if (testIndex==-1){ 

                incompleteObjects.get(index).addKeypoint(test);
            }


            else{
                incompleteObjects.get(index).joinObject(incompleteObjects.get(testIndex));
                incompleteObjects.remove(testIndex);
            }
            return true;
        }
        return false;
    }

    /**
     * Method to find the next smallest yMax from incompleteObjects.
     *
     * @param currentY The current y index. 
     * @return the index of the smallest yMax that is greater than current index.
     */
    private int getNextYmax(int currentY){
        int Ymax = listOfImages.get(0).getTileHeight()+1;
        if(incompleteObjects==null){return -1;}
        for (int i=0; i < incompleteObjects.size(); i++){
            if (currentY < incompleteObjects.get(i).yMax && incompleteObjects.get(i).yMax < Ymax){
                Ymax = incompleteObjects.get(i).yMax;
            }
        }
        return Ymax;
    }

    /**
     * Basic implementation of the absolute value math function.
     * @param i input value.
     * @return the absolute value of i
     */
    private int ABS(int i){
        if (i < 0){return -i;}
        return i;
    }

    /**
     * Updates coordinates of all pixels, typically called when a 
     * new image is being processed from listOfImages. 
     */
    private void iterateObjects(){
        // call iterate coords on all objects.
        if (incompleteObjects!=null){
            for(int i=0; i<incompleteObjects.size(); i++){
                incompleteObjects.get(i).iterateCoords(1);
            }
        }
        if (incompleteObjects!=null){
            for(int i=0; i<miscObjects.size(); i++){
                miscObjects.get(i).iterateCoords(1);
            }
        }
        if (cylinders!=null){
            for(int i=0; i<cylinders.size(); i++){
                cylinders.get(i).iterateCoords(1);
            }
        }
    }

    /**
     * Runs checkCylinder on all objects in incompleteObjects
     */
    private void flushIncompleteObjects(){
        while (incompleteObjects.size()>0){
            int index;
            if (!(incompleteObjects.get(0).getKeypoints().get(0)[0]<0)){index = listOfImages.size()-1;}
            else {
                index = listOfImages.size()-2+incompleteObjects.get(0).getKeypoints().get(0)[0];
                incompleteObjects.get(0).rewind(1-incompleteObjects.get(0).getKeypoints().get(0)[0]);
            }
            if (incompleteObjects.get(0).checkCylinder(listOfImages.get(index))){
                if (incompleteObjects.get(0).getSize()>0){cylinders.add(incompleteObjects.get(0));}
            }
            else{
                if (incompleteObjects.get(0).getKeypoints().size()>1){miscObjects.add(incompleteObjects.get(0));}
            }
            incompleteObjects.remove(0);
        }
    }

    /**
     * Prints basic information to terminal and calls file writer to archive more detailed results.
     */
    private void writeResults(){
        System.out.println("Cylinders: " + cylinders.size());
        System.out.println("Misc: " + miscObjects.size());

        dataHandler data = new dataHandler(cylinders, miscObjects, directory); 
        data.compileResults(outputFile);  // Send Object Info to datahandler
        imageOut(cylinders,miscObjects,incompleteObjects);
    }
   



    /**
     * Runs through incomplete objects and ensures no ajacent pixels belong to separate objects.
     */
    private void errorCheck(){
        if (incompleteObjects.size() > 1){
            for (int i = 0; i < incompleteObjects.size()-1; i++){
                int obIxMax = incompleteObjects.get(i).getKeypoints().get(incompleteObjects.get(i).getKeypoints().size()-1)[0];
                for (int k = incompleteObjects.get(i).getKeypoints().size() - 1; k >= 0; k--){
                    if ((incompleteObjects.get(i).getKeypoints().get(k)[0]!=obIxMax)){break;}
                    int[] comp = objects.offsetPoint(incompleteObjects.get(i).getKeypoints().get(k), 1, 0);
                    for (int j = i+1; j < incompleteObjects.size(); j++){          
                        if (comp[1] >= incompleteObjects.get(j).yMin && comp[1] <= incompleteObjects.get(j).yMax){
                            if (incompleteObjects.get(j).compare(comp)){
                                if (incompleteObjects.get(i).joinObject(incompleteObjects.get(j))){incompleteObjects.remove(j);}
                            }
                        }                   
                    }
                }
            }
        }
    }

    /**
     * method returns array with size/colour association
     * @return returns ArrayList(int[]) containing size and colours values;
     */
    public ArrayList<Integer> ArraySizesCol()
    {   ArrayList<int[]> ret=null;
        ret=new ArrayList<>();
        ArrayList<Integer> sizes = new ArrayList();
        for (int i = 0; i < cylinders.size(); i++) {
            if(!sizes.contains((int)cylinders.get(i).size))
            {
                sizes.add((int)(cylinders.get(i).size));
            }
          
        }
        return sizes;
    }

        /**generates output image with all cylinders pixels set to red and all misc objects outlined in blue
         * 
         * @param complete Array of objects that do classify as a cylinder
         * @param obj Array of objects that does not classify as cylinders
         * @return boolean if image output was successful
         */
        private boolean imageOut(ArrayList<objects> complete, ArrayList<objects> obj, ArrayList<objects> incomp){   
        // System.out.println("image Output called");
        Color[] cl= new Color[]{new Color(16,123,230),new Color(255,182,0),new Color(255,0,0),new Color(162,0,180),new Color(152,8,45),new Color(150,25,88),new Color(255,0,167),new Color(111,127,19),new Color(16,178,143),new Color(242,103,151)};
        ArrayList<Integer> ar= ArraySizesCol();
        Color colour = new Color(255,0,0);
        int col =colour.getRGB();
        colour = new Color(0,255,0);
        int col2 =colour.getRGB();
        colour = new Color(0,0,255);
        int col3 =colour.getRGB();
        colour = new Color(0,255,255);
        int col4 =colour.getRGB();
        if (complete.size()>0){
            for (int i = 0; i < complete.size(); i++) {
                
                for (int j2 = 0; j2 < ar.size(); j2++) {
                    
                    if(complete.get(i).size==ar.get(j2))
                     {
                        col=cl[j2].getRGB();
                     }
                }    
                 for (int j = 0; j <complete.get(i).keyPoints.size() ; j++){
                    
                    
                    int x1=(complete.get(i).keyPoints.get(j)[0] + listOfImages.size()-2);
                    int y1=complete.get(i).keyPoints.get(j)[1];
                    rep.setRGB(x1,y1,col);
                }
                for (int[] coord : complete.get(i).edges){
                    int x1=(coord[0] + listOfImages.size()-2);
                    int y1=coord[1];
                    rep.setRGB(x1,y1,col4);
                }
            }
        }
        if (miscObjects.size()>0){
            for (int i = 0; i < miscObjects.size(); i++) {
                // System.out.println(i+ " misc object size "+miscObjects.get(i).keyPoints.size());
                if (i%2==1){
                    for (int j = 0; j <miscObjects.get(i).keyPoints.size() ; j++){
                        int x1=(miscObjects.get(i).keyPoints.get(j)[0] + listOfImages.size()-2);
                        int y1=miscObjects.get(i).keyPoints.get(j)[1];
                        rep.setRGB(x1,y1,col2);   
                    }
                }
                else{
                    for (int j = 0; j <miscObjects.get(i).keyPoints.size() ; j++){
                        int x1=(miscObjects.get(i).keyPoints.get(j)[0] + listOfImages.size()-2);
                        int y1=miscObjects.get(i).keyPoints.get(j)[1];
                        rep.setRGB(x1,y1,col2);
                    }
                }
                for (int[] coord : miscObjects.get(i).edges){
                    int x1=(coord[0] + listOfImages.size()-2);
                    int y1=coord[1];
                    rep.setRGB(x1,y1,col4);
                }
            }

        }
        File x = new File("Output/"+directory+"Output.gif"); 
        try{
                ImageIO.write(rep, "gif", x); 
        }
        catch(Exception e)
        {
                System.out.println(e);
        }
        return true;
    }

} 