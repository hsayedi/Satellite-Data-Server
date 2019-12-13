import edu.ucr.cs.bdlab.geolite.Envelope;
import edu.ucr.cs.bdlab.geolite.IFeature;
import edu.ucr.cs.bdlab.geolite.IGeometry;
import edu.ucr.cs.bdlab.io.SpatialInputFormat;
import edu.ucr.cs.bdlab.raptor.Collector;
import edu.ucr.cs.bdlab.raptor.HDF4Reader;
import edu.ucr.cs.bdlab.raptor.Statistics;
import edu.ucr.cs.bdlab.raptor.ZonalStatistics;
import edu.ucr.cs.bdlab.sparkOperations.SpatialReader;
import edu.ucr.cs.bdlab.util.UserOptions;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Husna Sayedi in 2019
 */

public class ProcessZonalStatistics {

    public static ArrayNode filterCoordinatesDates(String rasterDirectory, String vectorFileName,
                                                   String minX, String maxX, String minY, String maxY, JavaSparkContext sc,
                                                   String startDate, String endDate) throws IOException {
        long t0 = System.nanoTime();
        // 1. Create a default SparkContext

        UserOptions opts = new UserOptions();

        // 2.5 Parse the coordinates received from the frontend
        double xMin, yMin, xMax, yMax;
        xMin = Double.parseDouble(minX.trim());
        xMax = Double.parseDouble(maxX.trim());
        yMin = Double.parseDouble(minY.trim());
        yMax = Double.parseDouble(maxY.trim());
        String coordinates = xMin + ", " + yMin + ", " + xMax + ", " + yMax;

        // 2. Load the polygons from shapefile
        opts.set(SpatialInputFormat.FilterMBR, coordinates); // make a string
        JavaRDD<IFeature> polygons = SpatialReader.readInput(sc, opts, vectorFileName, "shapefile");
        List<IFeature> features = polygons.collect();


        System.out.println("PROCESSING COORDINATES");

        // 3. Reproject to Sinusoidal space
        Envelope mbr = new Envelope(2); // empty envelope
        for (IFeature f : features) {
            HDF4Reader.wgsToSinusoidal(f.getGeometry());
            mbr.merge(f.getGeometry()); // gets features from
        }

        // 4. Locate all dates for the raster data and create a date filter
        //String startDate = "2018.01.01";
        // String endDate = "2018.01.03";
        Path rasterPath = new Path(rasterDirectory); //
        FileSystem rFileSystem = rasterPath.getFileSystem(opts);
        FileStatus[] matchingDates = rFileSystem.listStatus
                (rasterPath, HDF4Reader.createDateFilter(startDate, endDate));


        // 5. Select all files under the matching dates, select the files that match the MBR of the polygons
        List<Path> allRasterFiles = new ArrayList<>();
        for (FileStatus matchingDir : matchingDates) {
            FileStatus[] matchingTiles = rFileSystem.listStatus(matchingDir.getPath(),
                    HDF4Reader.createTileIDFilter(new Rectangle2D.Double(mbr.minCoord[0], // xMin
                            mbr.minCoord[1], mbr.getSideLength(0), mbr.getSideLength(1)))); // yMin, width, height
            for (FileStatus p : matchingTiles)
                allRasterFiles.add(p.getPath());
        }


        // 5. Initialize the list of geometries and results array
        IGeometry[] geometries = new IGeometry[features.size()];
        Statistics[] finalResults = new Statistics[features.size()];
        for (int i = 0; i < features.size(); i++) {
            geometries[i] = features.get(i).getGeometry();
            finalResults[i] = new Statistics();
            finalResults[i].setNumBands(1);
        }


        // 6. Run the zonal statistics operation
        HDF4Reader raster = new HDF4Reader();
        for (Path rasterFile : allRasterFiles) { // read in raster files
            raster.initialize(rFileSystem, rasterFile, "LST_Day_1km");
            Collector[] stats = ZonalStatistics.computeZonalStatisticsScanline(raster, geometries, Statistics.class);
            // Merge the results
            for (int i = 0; i < stats.length; i++) {
                if (stats[i] != null)
                    finalResults[i].accumulate(stats[i]);
            }
            raster.close();
        }

        long t1 = System.nanoTime();
        // 7. Print out the results
        System.out.println("Average Temperature (Kelvin)\tState Name");

        // put results in JSON object
        ObjectMapper jsonMapper = new ObjectMapper();
        ArrayNode response = jsonMapper.createArrayNode();

        for (int i = 0; i < geometries.length; i++) {
            if (finalResults[i].count[0] > 0) {
                ObjectNode objectCalled = jsonMapper.createObjectNode();
                //datafile, query, fieldInput, valueInput
                objectCalled.put("NAME", features.get(i).getAttributeValue("NAME").toString());
                objectCalled.put("sum", finalResults[i].sum[0]);
                objectCalled.put("count", finalResults[i].count[0]);
                objectCalled.put("average", finalResults[i].sum[0] / finalResults[i].count[0]);
                objectCalled.put("min", finalResults[i].min[0]);
                objectCalled.put("max", finalResults[i].max[0]);
                response.add(objectCalled); // add object to array of all results
                //System.out.println(finalResults[i]); //shows min, max, sum, count
                System.out.printf("%f\t%s\n",
                        finalResults[i].sum[0] / finalResults[i].count[0],
                        features.get(i).getAttributeValue("NAME"));
            }
        }
        //System.out.print(response);
        //return finalResults;
        long processingTime = t1 - t0;
        double elapsedTimeInSecond = (double) processingTime / 1E9;
        String returnStr = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        System.out.println(returnStr);
        System.out.println("System processing time (s): " + elapsedTimeInSecond);
        System.out.println("Number of polygons: " + response.size());
        // print number of pixels
        double count = 0;
        Iterator<JsonNode> elements = response.getElements();
        while(elements.hasNext()) {
            JsonNode next = elements.next();
            JsonNode count1 = next.get("count");
            count += count1.getDoubleValue();
        }

        System.out.println("Total count of pixels: " + count);
        return response;

    }
}
