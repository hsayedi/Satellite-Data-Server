/*
 * Copyright 2018 University of California, Riverside
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import edu.ucr.cs.bdlab.geolite.Envelope;
import edu.ucr.cs.bdlab.geolite.IFeature;
import edu.ucr.cs.bdlab.geolite.IGeometry;
import edu.ucr.cs.bdlab.raptor.Collector;
import edu.ucr.cs.bdlab.raptor.HDF4Reader;
import edu.ucr.cs.bdlab.raptor.Statistics;
import edu.ucr.cs.bdlab.raptor.ZonalStatistics;
import edu.ucr.cs.bdlab.sparkOperations.SpatialReader;
import edu.ucr.cs.bdlab.util.UserOptions;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
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
 * This file is used to test the ZonalStatistics operations
 * Without having to run the server and see results in the front-end
 * Note: some parameters are hard-coded here for testing purposes
 *
 * Runs a simple zonal statistics operation.
 * For further instructions check:
 * https://bitbucket.org/eldawy/beast-examples/src/master/doc/zonal-statistics.md
 */
public class TestZonalStatistics {

    public static void main(String[] args) throws IOException {
        // 1. Create a default SparkContext
        JavaSparkContext sc = new JavaSparkContext("local[*]", "test");
        UserOptions opts = new UserOptions();

        // 2. Load the polygons
        JavaRDD<IFeature> polygons = SpatialReader.readInput(sc, opts, "/Users/husnasayedi/Documents/UCR/MastersProject/Projecthadoop-horus-original/data/WGS84_boundaries/us_states.shp", "shapefile");
        List<IFeature> features = polygons.collect();


        String coordinates = "149.06285646316485, -149.06214353683515, -80.17886449617491, 80.17856249398272";
        String[] c = coordinates.split(",");
//        for (String i: c) {
//            System.out.println(i);
//        }
//        System.out.println();

        double xMin, yMin, xMax, yMax, width, height;
        xMin = Double.parseDouble(c[0].trim());
        xMax = Double.parseDouble(c[1].trim());
        yMin = Double.parseDouble(c[2].trim());
        yMax = Double.parseDouble(c[3].trim());


        // 3. Reproject to Sinusoidal space
        Envelope mbr = new Envelope(2, xMin, yMin, xMax, yMax);
        for (IFeature f : features) {
            HDF4Reader.wgsToSinusoidal(f.getGeometry());
            mbr.merge(f.getGeometry());
        }


        // 4. Locate all dates for the raster data
        String startDate = "2018.01.01";
        String endDate = "2018.01.03";
        Path rasterPath = new Path("/Users/husnasayedi/Documents/UCR/MastersProject/FinalProject/data/raster"); // "raster"
        FileSystem rFileSystem = rasterPath.getFileSystem(opts);
        FileStatus[] matchingDates = rFileSystem.listStatus
                (rasterPath, HDF4Reader.createDateFilter(startDate, endDate));


        // 5. Select all files under the matching dates
        List<Path> allRasterFiles = new ArrayList<>();
        for (FileStatus matchingDir : matchingDates) { // comment out for orig
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
        for (Path rasterFile : allRasterFiles) {
            raster.initialize(rFileSystem, rasterFile, "LST_Day_1km");
            Collector[] stats = ZonalStatistics.computeZonalStatisticsScanline(raster, geometries, Statistics.class);
            // Merge the results
            for (int i = 0; i < stats.length; i++) {
                if (stats[i] != null)
                    finalResults[i].accumulate(stats[i]);
                //System.out.println(stats[i]); // shows sum, count, max, min
            }
            raster.close();
            //System.out.println(Arrays.toString(finalResults));
            //System.out.println(geometries.length); // num states = 49
        }

        // 7. Print out the results and put into JSON array
        System.out.println("Average Temperature (Kelvin)\tState Name");
        // put results in JSON object
        ObjectMapper jsonMapper = new ObjectMapper();
        ArrayNode response = jsonMapper.createArrayNode();

        for (int i = 0; i < geometries.length; i++) {
            if (finalResults[i].count[0] > 0) {
                ObjectNode objectCalled = jsonMapper.createObjectNode();
                //for (i in finalResults)

                //datafile, query, fieldInput, valueInput
                objectCalled.put("NAME", features.get(i).getAttributeValue("NAME").toString());
                objectCalled.put("sum", finalResults[i].sum[0]);
                objectCalled.put("count", finalResults[i].count[0]);
                objectCalled.put("average", finalResults[i].sum[0] / finalResults[i].count[0]);
                objectCalled.put("min", finalResults[i].min[0]);
                objectCalled.put("max", finalResults[i].max[0]);

                System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectCalled));
                response.add(objectCalled); // add object to array of all results
                //System.out.println(finalResults[i]); //shows min, max, sum, count
                System.out.printf("%f\t%s\n",
                        finalResults[i].sum[0] / finalResults[i].count[0],
                        features.get(i).getAttributeValue("NAME"));
            }
        }
        System.out.println("Full JSON response:");
        System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        System.out.println("Number of polygons: " + response.size());
        // print count sum
        JSONArray array = new JSONArray();
        JSONObject obj = new JSONObject();

        double count = 0;
        Iterator<JsonNode> elements = response.getElements();
        while(elements.hasNext()) {
            JsonNode next = elements.next();
            JsonNode count1 = next.get("count");
            count += count1.getDoubleValue();
        }

        System.out.println(count);


        // Clean up
        sc.close();

    }
}
