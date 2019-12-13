import org.apache.commons.cli.*;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.tika.parser.DefaultParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

/**
 * Created by Husna Sayedi in 2019
 */
public class ProcessSatelliteData {
    /**
     * Possible methods to run a zonal statistics query.
     */
    public static Options getCommandLineOptions() {
        Options options = new Options();

        Option rasterDirectory = new Option("rd", "raster  directory", true,"HDF raster files directory");
        rasterDirectory.setRequired(true);
        options.addOption(rasterDirectory);

        Option vinput = new Option("v", "vector", true, "vector file path");
        vinput.setRequired(true);
        options.addOption(vinput);

        Option query = new Option("q", "query", true, "query type to run");
        options.addOption(query);

        Option minX = new Option("minX", "xMin", true, "coordinates from front end");
        options.addOption(minX);

        Option maxX = new Option("maxX", "xMax", true, "coordinates from front end");
        options.addOption(maxX);

        Option minY = new Option("minY", "yMin", true, "coordinates from front end");
        options.addOption(minY);

        Option maxY = new Option("maxY", "yMax", true, "coordinates from front end");
        options.addOption(maxY);

        Option startDate = new Option("sd", "startDate", true, "start date from front end");
        options.addOption(minY);

        Option endDate = new Option("ed", "endDate", true, "end date from front end");
        options.addOption(maxY);

        return options;
    }

    /**
     * Note: query 'allpolys' specifies all polygons visible in the interface at a given zoom level
     * This method receives user-specified parameters from the frontend to send to backend for query processing
     */

    public String data(String query, String vectorFileName, String rasterDirectory, JavaSparkContext sc,
                       String minX, String maxX, String minY, String maxY,
                       String startDate, String endDate) throws IOException, TransformException {


        ObjectMapper jsonMapper = new ObjectMapper();


        if (query.equals("allpolys")) {
            ArrayNode response =  ProcessZonalStatistics.filterCoordinatesDates(rasterDirectory, vectorFileName,
                    minX, maxX, minY, maxY,
                    sc,
                    startDate, endDate);
            String returnStr = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            //System.out.println(returnStr);
            return returnStr;
        }
        else {
            throw new RuntimeException("Unknown query '" + query + "'");
        }

    }


    public static void main(String[] args) throws IOException, TransformException {

        Options options = getCommandLineOptions();
        CommandLineParser parser = (CommandLineParser) new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ZonalStatistics", options);
            System.exit(1);
            return;
        }

        String vectorFileName = cmd.getOptionValue('v');
        String query = cmd.getOptionValue('q', "allpolys").toLowerCase();
        String minX = cmd.getOptionValue("minX", null);
        String maxX = cmd.getOptionValue("maxX", null);
        String minY = cmd.getOptionValue("minY", null);
        String maxY = cmd.getOptionValue("maxY", null);
        String startDate = cmd.getOptionValue("startDate", "01.01.18"); // testing with default value
        String endDate = cmd.getOptionValue("endDate", "01.03.18"); // testing with default value
        String rasterDirectory = cmd.getOptionValue("rd");


        if (query.equals("allpolys")) {
            JavaSparkContext sc = new JavaSparkContext("local[*]", "test");
            ProcessZonalStatistics.filterCoordinatesDates(rasterDirectory, vectorFileName, minX, maxX, minY, maxY, sc, startDate, endDate);

        } else {
            throw new RuntimeException("Unknown query '" + query + "'");
        }

    }


}








