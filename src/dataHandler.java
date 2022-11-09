/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * class compiles data input and writes it to a file for storage.
 *
 * @author storm
 */
public class dataHandler {
    ArrayList<objects> cylinders;
    ArrayList<objects> miscObjects;
    String dirName;
    double size = 0;
    int objects = 0;
    String out;

    /**
     * creates a dataHandler object
     *
     * @param cyl cylinder ArrayList.
     * @param obj misc. objects ArrayList.
     * @param dir String Containing directory name.
     */
    public dataHandler(ArrayList<objects> cyl, ArrayList<objects> obj, String dir) {

        cylinders = cyl;
        miscObjects = obj;
        dirName = dir;

    }

    /**
     * Compiles and sorts all results and writes it to text file
     *
     * @param fl path of file to write results to
     */
    public void compileResults(String fl) {
        String file;
        System.out.println("compiling Results...");
        FileWriter outputFile = null;

        if ((null == cylinders) && (cylinders.isEmpty())) {
            System.out.println("EMPTY ARRAYLIST RECEIVED");
        } else {

            objects = miscObjects.size();// objects left after scan.
            BufferedWriter writer = null;
            try {

                file ="Output/Output.txt";
                outputFile = new FileWriter(file, true);

                ArrayList<int[]> results = new ArrayList<int[]>();
                for (int i = 0; i < cylinders.size(); i++) {

                    // go through cylinder Array and group different size cylinders together
                    size = cylinders.get(i).getSize(); // gets size of current cylinder in array
                    boolean flg = false;
                    for (int j = 0; j < results.size(); j++) {// loop through recorded sizes for match
                        if (size == results.get(j)[0]) {// if size matches

                            int[] AR = results.get(j);// temp array to implement changes
                            AR[1] = results.get(j)[1] + 1;// increment size count in array
                            results.set(j, AR);// implements change to arrayList
                            flg = true;
                            break;
                        }

                    }
                    if (flg == false) {
                        results.add(new int[] { (int) size, 1 });
                    }

                }

                // writing line to file
                String lineDir = dirName + ",";

                results = sort(results);
                for (int x = 0; x < results.size(); x++) {
                    lineDir += results.get(x)[0] + "/" + results.get(x)[1] + ",";
                }
                lineDir += objects;
                writer = new BufferedWriter(outputFile);
                if (checkDuplicates(lineDir, file) == false) {
                    writer.write(lineDir+"\n");
                }

            } catch (FileNotFoundException ex) {
                Logger.getLogger(dataHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

            catch (IOException x) {
                System.out.println(x);
                x.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                        outputFile.close();
                    } catch (IOException x) {
                        System.out.println(x);
                    }
                }

            }
        }
    }
    /**
     *  method used to sort an ArrayLists results
     * @param results unsorted ArrayList<int[]>
     * @return ArrayList<int[]> that has been sorted
     */
    public ArrayList<int[]> sort(ArrayList<int[]> results) {
        for (int i = 0; i < results.size() - 1; i++) {
            int[] c1 = results.get(i);
            int[] c2 = results.get(i + 1);
            if (c1[0] > c2[0]) {
                results.set(i + 1, c1);
                results.set(i, c2);
            }

        }
        return results;
    }

    /**
     * returns a String containing latest Production information extracted from File
     * Path input as a parameter.
     * 
     * @param path path of file containing production history
     * @return String containing latest Production information extracted from File
     *         Path.
     */
    static public String getLatest(String path) {
        String line = "";
        Scanner sc = null;
        try {
            sc = new Scanner(new File(path));
            while (sc.hasNextLine()) {
                line = sc.nextLine();
            }
            sc.close();
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (sc != null) {
                sc.close();
            }
        }

        return line;
    }
/**
 * checkDuplicates used to check a file for duplicates
 * @param line line that needs to be checked for in the file
 * @param path String path of file to check
 * @return boolean true if line is found in file else false
 */
    public  boolean checkDuplicates(String line,String path)
    {    Scanner scan=null;
        try{
         scan = new Scanner(new File(path));
         do{
             if(scan.hasNextLine())
             {
                String str=scan.nextLine();
               
            
                if(str.compareTo(line)==0)
                {
                   
                    return true;
                }
            }
            
            
         }
         while(scan.hasNextLine());

        }
        catch(Exception x)
        {
            x.printStackTrace();
        }
        finally
        {
            if(scan!=null)
            {
                scan.close();
            }
        }
        
        return false;
    }

}
