# Satellite-Date-Server: Ad-Hoc Querying of Satellite Data to Process Multiple Polygons


## Background 

The Satellite-Data-Server hosts satellite data to provide two main functions: 
(1) Processing the data in aggregate using the Scanline method and 
(2) Hosting the data to make available for users through a map interface. 

We execute the zonal statistics operation using the Scanline method. This method processes queries in aggregation, which 
combines vector and raster data in their raw formats. We use these techniques to process multiple polygons at once, specified 
by the user. This allows for greater usability and to ensure efficiently processed polygons of interest. 

This system will allow users to query multiple polygons simultaneously based on what is visible on the map. 
These include different zoom-level granularities, e.g. state, county, and zip code level polygons. The backend server 
will compute a range of statistics: sum and count (of pixels), and min, max, average (of temperature in Kelvins). 



## Run the Application

1) Run the backend server. Navigate to src/main/java/SatelliteDataServer.java and run. 

2) Start the node server. Navigate to map_interface/ directory. In terminal, run ```npm start``` to start the server.
You will see the server running at a given URL, shown in the image below. Copy and paste the URL into a browser. 

3) In the web browser, add select StartDate and EndDate. Because the sample of raster data is from January 1, 2018 
to January 6, 2018, make sure to select a dates in that range. 
Example: 
Select January 1, 2018 as StartDate
Select today's date as EndDate

4) Zoom in (one zoom at a time) and you should see some state colors change. 

Purple inidicates an undefinied region, which is due to the small range of sample data we are using. You will notice a 
gradience of green to orange states. These shades indicate the aggregate temperatures for each given polygon. 

5) Zoom in a few more times until you start to see county polygons. Again, these counties will be color-coded according
to their average temperatures. 



