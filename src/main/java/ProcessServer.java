import org.apache.htrace.fasterxml.jackson.databind.JsonNode;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.api.java.JavaSparkContext;
import org.opengis.referencing.operation.TransformException;

import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Husna Sayedi in 2019
 */
public class ProcessServer extends HttpServlet {

    JavaSparkContext sc = null;
    public void init() {
        sc = new JavaSparkContext("local[*]", "test");
    }
    public void destroy() {
        if(sc != null) {
            sc.close();
        }
    }

    ObjectMapper mapper = new ObjectMapper();
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
        String body = getBody(req);
        JsonNode object = mapper.readTree(body);
        JsonNode fieldName = object.get("fieldName");
        String s = fieldName.asText();
        System.out.println(s);

    }

    public static String getBody(HttpServletRequest request) throws IOException {

        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        return body;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        resp.setStatus(HttpServletResponse.SC_OK);

        String query = req.getParameter("query");
        String minX = req.getParameter("minX");
        String minY = req.getParameter("minY");
        String maxX = req.getParameter("maxX");
        String maxY = req.getParameter("maxY");
        String vectorFile = req.getParameter("vectorFile");

        String startDate = req.getParameter("startDate");
        String endDate = req.getParameter("endDate");

        ProcessSatelliteData stats = new ProcessSatelliteData();

        String result = "";
        //query = "allpolys";
        try {

            ///Users/husnasayedi/Documents/UCR/MastersProject/Projecthadoop-horus-original/data/c/us_states.shp

            String vectorFileName = vectorFile;
            String rasterDirectory = "/Users/husnasayedi/Documents/UCR/MastersProject/Satellite-Data-Server/data/raster/";

//            if(query == null){
//                query = "allpolys";
//            }
//
//            else {
//                throw new RuntimeException("Unknown query '" + query + "'");
//            }

//            ArrayNode response =  ProcessZonalStatistics.filterCoordinatesDates(rasterDirectory, vectorFileName,
//                    minX, maxX, minY, maxY, sc, startDate, endDate);


            //    public static ArrayNode filterCoordinatesDates(String rasterDirectory, String vectorFileName,
            //                                                   String minX, String maxX, String minY, String maxY, JavaSparkContext sc,
            //                                                   String startDate, String endDate) throws IOException {

            result = stats.data(
                    query,
                    vectorFileName,
                    rasterDirectory,
                    sc,
                    minX,
                    maxX,
                    minY,
                    maxY,
                    startDate,
                    endDate
            );



        } catch (TransformException e) {
            e.printStackTrace();
        }

        PrintWriter out = resp.getWriter();
        // Results being printed in browser
        out.println(result);

    }


}


