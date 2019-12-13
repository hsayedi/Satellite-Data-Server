# Map Interface


## Brief Overview 
This map interface uses the OpenLayers API. We use the output of the Zonal Statistics Server.
These results give us statistics for each polygon (a state or county) as well as a total aggregate for the region. These statistics
comprise of the sum, count, min, max, and average.




## How to Use
Navigate to directory and run:

`npm start`

In your browser, navigate to the given URL (i.e. http://localhost:1234)


## Output
As you zoom into regions within the US, if you zoom in enough the polygons change from states to counties. 
The coordinates of the rectangle change in the bottom boxes as you zoom in and out. There are two calendars which can take in
a start date and an end date, although this information is not currently being utilized by the backend server.

A view of the states: 
![alt text](https://github.com/hsayedi/zonal_statistics_server/blob/master/data/map_interface/states_zoom.png)

A view of the counties: 
![alt text](https://github.com/hsayedi/zonal_statistics_server/blob/master/data/map_interface/counties_zoom.png)


Because of problems with the filter in the backend, this version of hte code is still using a hardcoded file 'results.json' 
(which includes zonal statistics of a few selected states states) to compute average temperatures. 
The average temperatures from this file are in converted from Kelvins to Ferenheit. 

Once the filter is working, we will be able to properly see statistics and colors for each polygon, depending on what polygons 
you see in the map and the zoom level. 

Right now, the backend server outputs the an aggregation of zonal statistics which looks like the following:
![alt text](https://github.com/hsayedi/zonal_statistics_server/blob/master/data/map_interface/agg_zonal_stats.png)

The states which are a darker shade of red indicate an overall higher average temeprature (90+), while the yellow shaded 
states have median temperatures (80-89). The green shaded states have cooler temperatures (60-70). Purple states are either 
not contained in the region or was not in our Zonal Statistics output. 


