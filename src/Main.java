import java.io.*;
import java.util.*;
import java.util.List;

/**
 * @author eastonsmith
 * Student # : 300189637
 *
 * This is the main class for the TaxiCluster java project.
 * In this class are all the methods relating to running the DBSCAN algorithm and a method for
 * gathering data from a .csv file into an ArrayList for the DBSCAN algorithm.
 *
 * -> https://cse.buffalo.edu/~jing/cse601/fa13/materials/clustering_density.pdf
 * above is a powerpoint that includes the pseudo-code my DBSCAN algorithm is based off
 */
public class Main {

    protected List<TripRecord> tripRecords; // array list of trip record, for clustering
    protected List<Cluster> clusters; // array list to store all clusters in
    protected float eps;
    protected int minPts;

    public Main(){
        this.clusters = new ArrayList<>(); //  create new arrayList of clusters
    }

    // Method just for taking inputs and assigning those inputted values to eps and minPts.
    private void inputs(){
        Scanner scan = new Scanner(System.in);
        System.out.println("Input minPts: ");
        this.minPts = scan.nextInt();
        System.out.println("Input Epsilon: ");
        this.eps = scan.nextFloat();
    }

    /**
     * Calculates the euclidean distance between 2 points
     * @param p1
     * first GPS coordinate to find distance between
     * @param p2
     * first GPS coordinate to find distance between
     * @return
     * the euclidean distance between p1 and p2
     */
    private float euclidDist(GPScoord p1, GPScoord p2){
        float x = (p2.getLat()-p1.getLat());
        float y = (p2.getLon()-p1.getLon());

        float dist = (x*x) + (y*y);
        return (float) Math.sqrt(dist);
    }

    /**
     * Implementation of DBSCAN algorithm using ArrayList as data type for containing the data points.
     * @param db
     * An ArrayList, containing all the tripRecord data points for clusters
     * @param eps
     * The maximum distance that can be between points in order for them to still be
     * considered in the same cluster.
     * @param minPts
     * The minimum number of points that need to be grouped up in order for the algorithm
     * to consider that group a cluster.
     */
    private void dbScan(List<TripRecord> db, float eps, int minPts){
        int c = 0;
        for (TripRecord tr : db){
            if(tr.getVisited()) { continue; }

            tr.visit();
            List<TripRecord> neighbours = regionQry(tr, eps);
            if(neighbours.size() < minPts) {
                tr.noise();
                continue;
            }
            Cluster clstr =  new Cluster(++c);
            expandCluster( tr, neighbours, clstr, eps, minPts );
            clstr.setCenter();
            this.clusters.add(clstr);
        }
    }

    /**
     * Method for expanding and adding points to a cluster
     * @param p
     * Central point of cluster to expand.
     * @param neighbours
     * Arraylist containing all neighbour points to @p
     * @param c
     * The specific cluster to expand/add points to
     * @param eps
     * The maximum distance that can be between points in order for them to still be
     * considered in the same cluster.
     * @param minPts
     * The minimum number of points that need to be grouped up in order for the algorithm
     * to consider that group a cluster.
     */
    private void expandCluster(TripRecord p, List<TripRecord> neighbours, Cluster c, float eps, int minPts) {
        c.addGPS(p);
        List<TripRecord> seedSet = new ArrayList<>(neighbours);
        int i = 0;
        while( i < seedSet.size() ){
            TripRecord tr = seedSet.get(i++);
            if(tr.getCluster() == -1) c.addGPS(tr);

            if(!(tr.getVisited())){
                tr.visit();
                List<TripRecord> nPts = regionQry(tr, eps);
                if(nPts.size() >= minPts){
                    seedSet.addAll(nPts);
                }
            }
        }
    }

    /**
     * Method to find all the points around a central point, p, that are within
     * a given distance, eps, and returns them as a list
     * @param p
     * A point to be at the center of the region
     * @param eps
     * Maximum distance that can be between p and another point for that point to be in the region
     * @return
     * return the set of all points that are in the given eps of the point p in a list
     */
    private List<TripRecord> regionQry(TripRecord p, float eps){
        List<TripRecord> n = new ArrayList<>();
        for(TripRecord tr : this.tripRecords){
            if( euclidDist(p.getPickup_Location(), tr.getPickup_Location()) <= eps && tr!=p){
                n.add(tr);
            }
        }
        return n;
    }

    /**
     * Method reads a csv file into an array list,
     * each element in the array list is a row from the csv file
     * each element in the arrays in the list are the columns from the csv fil
     * @param f
     * File path for the csv file that is to be read
     */
    private void readCSV(String f){
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            this.tripRecords = new ArrayList<>();
            //String[] attr = br.readLine().split(",");

            String line;
            br.readLine(); // skips first line with attributes
            while ((line = br.readLine()) != null) {
                String[] lineArr = line.split(",");
                tripRecords.add(
                        new TripRecord(
                                lineArr[4], // index 4 is Trip_Pickup_DateTime
                                new GPScoord(Float.parseFloat(lineArr[8]), Float.parseFloat(lineArr[9])), // index 8/9 is start lon/lat
                                new GPScoord(Float.parseFloat(lineArr[12]), Float.parseFloat(lineArr[13])), // index 12/13 is end lon/lat
                                Float.parseFloat(lineArr[7]) // index 7 is the trip distance
                        )
                );
            }
        } catch(IOException ex){
            ex.printStackTrace();
        }
    }

    private void writeCSV(){
        try{
            BufferedWriter bw = new BufferedWriter(
                    new FileWriter(
                            String.format("out/TaxiClusterOut-%.5f-%d.csv", this.eps, this.minPts),
                            true
                    ));


            for(Cluster c : clusters){
                bw.write(c.printClust());
                System.out.println(c.printClust());
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Class' main method for executing the program
    public static void main(String[] args) {
        Main taxiCluster = new Main();

        // below runs dbscan and outputs
        taxiCluster.readCSV("data/yellow_tripdata_2009-01-15_1hour_clean.csv"); // reads the data from csv in tripRecords list
        taxiCluster.inputs();
        taxiCluster.dbScan(taxiCluster.tripRecords, taxiCluster.eps, taxiCluster.minPts);
        taxiCluster.writeCSV();
    }

}
