import java.util.Arrays;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.lang.Math.*;

/**
 * Stores object coordinate and classification data.
 *
 * @author Lucas
 */
public class objects {

    // Only initialized when object is finished read in.
    protected boolean cylinder;
    protected BufferedImage image;

    protected ArrayList<int[]> keyPoints; // Each int[] will contain (x,y)
    protected ArrayList<int[]> edges; // Each int[] will contain (x,y)
    protected int keyPointsIndex;
    protected int yMin;
    protected int yMax;

    protected double size;
    private int acceptAbleDelta = 180; 

    /**
     * Constructor for an object containing only 1 pixel.
     * @param coord coordinate of object.
     * @param keyPointsIndex Indcates which image the coordinate refers to.
     */
    public objects(int[] coord, int keyPointsIndex){
        this.keyPointsIndex = keyPointsIndex;
        keyPoints = new ArrayList<int[]>();
        int[] point = new int[]{coord[0], coord[1]};
        keyPoints.add(point);
        yMin = coord[1];
        yMax = coord[1];
        // System.out.println("New Object at (" + coord[0] + ", " + coord[1] + ") ImageIndex: " + keyPointsIndex);
    }

    /**
     * Checks if the object tracked is a cylinder.
     * @return boolean indicating if object is a cylinder.
     */
    public boolean checkCylinder(BufferedImage image){
        edges = new ArrayList<int[]>();
        this.image = image;
        cylinder = true;
        double minError = 0;
        double maxError = 0;
        double radEstimate= (yMax-yMin)/2;  // Radius estimate
        double errorMargin= 1; // + (radEstimate/11); //to be optimised.
        double yMidpoint=(yMax + yMin)/2;
        // Find xMin and xMax, take midpoints of (yMin, yMax) and (xMin, xMax) to create a "center."
        // Due to the scan starting on the top left of an object and working down a column, then shifting to the next x coordinate
        // before working down the new column, with every new pixel being appended, we can guarantee the min x value is at index 0 and the
        // Max x value is at the last index of the array.
        double xMidpoint = xMidpoint();
        for(int[] key:keyPoints){
            if (isEdge(key)){
                edges.add(key);
                //Compute the distance from the centre for each KeyPoint
                double distance=Math.sqrt( ((xMidpoint-key[0])*(xMidpoint-key[0])) + ((yMidpoint-key[1])*(yMidpoint-key[1])) );
                double error = distance-radEstimate;
                if (error<minError){minError=error;}
                else if (error>maxError){maxError=error;}
                // Check distance from center is within margin of error with radius estimate.
                if(ABS(distance-radEstimate) > errorMargin){cylinder = false;}
            }
        } // if not
        // Ensures the discrepancy between the smallest and largest radii measured is also within error margin. This rules out many hexagonal shapes.
        if (maxError-minError>errorMargin){cylinder = false;}
        size = 2 * radEstimate;

        return cylinder;
    }

    /**
     * Edge point checks vs 4 neighbours, if less than 4 sides are object, its an edge.
     *
     * @param point (x,y) coordinate of the point to check.
     * @return True if @param point is an edge to some object.
     */
    public boolean isEdge(int[] point){
        // Check if pixel to the left exists, and if so if its a background pixel. If so Pixel is an edge pixel.
        if (point[0]==0 || comparePixeltoInt(new int[]{point[0]-1, point[1]}, -16777216, acceptAbleDelta)){return true;}
        // Check if pixel above exists, and if so if its a background pixel. If so Pixel is an edge pixel.
        if (point[1]==0 || comparePixeltoInt(new int[]{point[0], point[1]-1}, -16777216, acceptAbleDelta)){return true;}
        // Check if pixel to the right exists, and if so if its a background pixel. If so Pixel is an edge pixel.
        if (point[0]==image.getTileWidth()-1 || comparePixeltoInt(new int[]{point[0]+1, point[1]}, -16777216, acceptAbleDelta)){return true;}
        // Check if pixel below exists, and if so if its a background pixel. If so Pixel is an edge pixel.
        if (point[1]==image.getTileHeight()-1 || comparePixeltoInt(new int[]{point[0], point[1]+1}, -16777216, acceptAbleDelta)){return true;}

        // Pixel is surrounded by object pixels and therefore not an edge pixel.
        return false;
    }
   
    /**
     * Moves Keypoint coords across the conveyor belt. 
     *
     * @param step The number of pixels to move all object coordinates.
     */
    public void iterateCoords(int step){
        for (int[] coord : keyPoints){
            coord[0] = coord[0]-1; // This may need to change depending on how pixel location is tracked with the gif.
        }
        keyPointsIndex = keyPointsIndex+1;
    }

    /**
     * Appends a new key point to the object.
     *
     * @param coord Coordinates of the new Keypoint, expected as [x,y]
     */
    public void addKeypoint(int[] coord){
        int[] point = new int[]{coord[0], coord[1]};
        keyPoints.add(point);
        MinAndMax(point[1]);
        // System.out.println("Added (" + point[0] + ", " + point[1] + ")");
    }

    /**
     * @return The diameter of the cylinder. 
     */
    public double getSize(){return size;}

    /**
     * @return Arraylist containing pixel coordinates of the object.
     */
    public ArrayList<int[]> getKeypoints(){
        return keyPoints;
    }

    /**
     * Function rewinds object value by x frames.
     * @param x the number of frames to move the object back by.
     */
    public void rewind(int x){
        keyPointsIndex = keyPointsIndex - x;
        for (int[] point : keyPoints){
            point[0] = point[0]+x;
        }
    }

    /**
     * Given a point, returns a new point that has value with x and y offsets equivalent to the given arguments.
     * @param point Base point, this value will be offset from.
     * @param offsetX Distance to offset the x Coordinate.
     * @param offsetY Distance to offset the y Coordinate.
     * @return The point after offsets have been applied.
     */
    public static int[] offsetPoint(int[] point, int offsetX, int offsetY){
        return new int[]{point[0]+offsetX, point[1]+offsetY};
    }

    /**
     * Connects this object to the provided object. Note neither object is deleted,
     * This object becomes the combined object and the argument object is left as is.
     * Method will not combine the objects if the keypoints index referenced by each object is not a match.
     * @param other The object to be combined.
     * @return Boolean representing success or failure of this operation.
     */
    public boolean joinObject(objects other){
        if (this.keyPointsIndex == other.keyPointsIndex){
            this.keyPoints.addAll(other.keyPoints); // Each int[] will contain (x,y)
            if (other.yMin < this.yMin){this.yMin = other.yMin;}
            if (other.yMax > this.yMax){this.yMax = other.yMax;}
            return true;
        }
        return false;
    }

    /**
     * Function to check if a point is contained in the Keypoints array.
     *
     * @param point Point being queried vs object.
     * @return True if point is in object.
     */
    public boolean compare(int[] point){
        // System.out.println("Comparing point and Points \npoint: ("+point[0]+", "+point[1]+")");
        for (int[] points : keyPoints){
            // System.out.println("Point: ("+points[0]+", "+points[1]+")");
            if (point[0]==points[0] && point[1]==points[1]){
                return true;
            }
        }
        return false;
    }

    /**
     * Updates yMin and yMax after a new point is added.
     *
     * @param newY A new y coordinate added to the object that needs to be 
     * checked vs the listed min and max values currently recorded for the object.
     */
    private void MinAndMax(int newY){
        if (newY < yMin){ yMin=newY; }
        else if (yMax < newY){ yMax=newY; }
    }

    /**
     * Compares pixel to an RGB value
     *
     * @param pixelValues First pixel to be compared. Expected as [x,y]
     * @param blackRGB RGB value of the background.
     * @param imageIndex The index from listOfImages of the image being processed currently.
     * @return Boolean thats true if pixel colour matches to withing a small margin of error.
     */
    private boolean comparePixeltoInt(int[] pixelValues, int blackRGB, int Delta){

        int pixelRGB=image.getRGB(pixelValues[0], pixelValues[1]);

        int blackRed = (blackRGB>>16) & 0xff;
        int pixelRed = (pixelRGB>>16) & 0xff;

        int blackGreen =(blackRGB>>8) & 0xff;
        int pixelGreen = (pixelRGB>>8) & 0xff;

        int blackBlue = (blackRGB) & 0xff;
        int pixelBlue = (pixelRGB) & 0xff;

        int deltaRed = ABS( blackRed-  pixelRed);
        int deltaGreen = ABS( blackGreen - pixelGreen);
        int deltaBlue = ABS(blackBlue - pixelBlue);

        if ((deltaBlue+deltaGreen+deltaRed)<Delta){
            return true;
        }
        return false;
    }

    /**
     * Basic implementation of the absolute value math function.
     * @param i input value.
     * @return the absolute value of i
     */
    private double ABS(double i){
        if (i < 0){return -i;}
        return i;
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
     * @param point to be returned as a string.
     * @return The point represent as (x, y)
     */
    private String returnPointAsString(double[] point){return "("+point[0]+", "+point[1]+")";}

    /**
     * @return the midpoint of xMin and xMax.
     */
    private double xMidpoint(){
        int xLower=keyPoints.get(0)[0];
        int xUpper=keyPoints.get(keyPoints.size()-1)[0];
        return (xUpper + xLower)/2;
    }

}