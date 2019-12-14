import * as fs from "fs";
import { toLonLat } from 'ol/proj.js';
import { getBottomLeft, getTopRight, getTopLeft, getBottomRight, getHeight, getWidth } from 'ol/extent.js';
import Feature from 'ol/Feature.js';
import Map from 'ol/Map.js';
import View from 'ol/View.js';
import GeoJSON from 'ol/format/GeoJSON.js';
import Circle from 'ol/geom/Circle.js';
import { Tile as TileLayer, Vector as VectorLayer } from 'ol/layer.js';
import { OSM, Vector as VectorSource } from 'ol/source.js';
import { Circle as CircleStyle, Fill, Stroke, Style } from 'ol/style.js';

// read in states and counties files
var contents = fs.readFileSync('states_all.geojson', 'utf8');
var contentsCounty = fs.readFileSync('counties_all.geojson', 'utf8');

// Need to replace this file with results calculated from the backend 
var results = fs.readFileSync('results.json', 'utf8');
var resObj = {};
//console.log(results);

// temperature to color map
var map = {};


$(document).ready(function () {
    var selection = 'state';
    // this function will change the color of each polygon depending on the average temp
    var initializeMap = function () {
        map[200] = { stroke: 'rgba(44, 190, 195, 4)', fill: 'rgba(44, 190, 195, 0.2)' };
        map[210] = { stroke: 'rgba(44, 135, 195, 4)', fill: 'rgba(44, 135, 195, 0.2)' };
        map[220] = { stroke: 'rgba(190, 44, 195, 4)', fill: 'rgba(190, 44, 195, 0.2)' };
        map[230] = { stroke: 'rgba(98, 203, 239, 4)', fill: 'rgba(98, 203, 239, 0.2)' };
        map[240] = { stroke: 'rgba(98, 239, 203, 4)', fill: 'rgba(98, 239, 203, 0.2)' };
        map[250] = { stroke: 'rgba(98, 239, 105, 4)', fill: 'rgba(98, 239, 105, 0.2)' };
        map[260] = { stroke: 'rgba(164, 239, 98, 4)', fill: 'rgba(164, 239, 98, 0.2)' };
        map[270] = { stroke: 'rgba(239, 224, 98, 4)', fill: 'rgba(239, 224, 98, 0.2)' };
        map[280] = { stroke: 'rgba(239, 178, 98, 4)', fill: 'rgba(239, 178, 98, 0.2)' };
        map[290] = { stroke: 'rgba(239, 124, 98, 4)', fill: 'rgba(239, 124, 98, 0.2)' };
        map[300] = { stroke: 'rgba(239, 98, 98, 4s)', fill: 'rgba(239, 98, 98, 0.2)' };
    }
    //console.log('Which file?' + contentsCounty);
    var geojsonObjectState = JSON.parse(contents);
    var geojsonObjectCounty = JSON.parse(contentsCounty);

    // gets the color for each temp
    var getColorForTemp = function (num) {
        var temp = num - (num % 10);
        var returnval = map[temp];
        return returnval;
    }

    // return the average temp of each polygon
    var returnAverageTemperature = function (Name) {
        if (resObj && resObj.length > 0) {                                                  // if data in results is not empty,
            for (var i = 0; i < resObj.length; i++) {  
                var NameFromResults = resObj[i].NAME.split(' ').join('_');    
                //console.log(Name+':'+NameFromResults);  
                if (NameFromResults === Name){
                    return resObj[i].average;
                }                
            }
        }
    }

    var display = function (id, value) {
        document.getElementById(id).value = value.toFixed(2);
    }

    var wrapLon = function (value) {
        var worlds = Math.floor((value + 180) / 360);
        return value - (worlds * 360);
    }

    // getting lat and long from openstreetmap
    var reverseGeocode = function (coords) {
        fetch('http://nominatim.openstreetmap.org/reverse?format=json&lon=' + coords[0] + '&lat=' + coords[1])
            .then(function (response) {
                return response.json();
            }).then(function (json) {
                console.log(json);
            });
    }

    let previousZoomLevel = 1;
    // fetches box from map - bottomLeft, topRight, topLeft, bottomRight coordinates
    var onMoveEnd = function (evt) {
        var map = evt.map;
        var zoomLevel = map.getView().getZoom();
        //console.log('Printing Zoom Level: ' + zoomLevel);
        var extent = map.getView().calculateExtent(map.getSize());

        var bottomLeft = toLonLat(getBottomLeft(extent));
        var topRight = toLonLat(getTopRight(extent));
        var topLeft = toLonLat(getTopLeft(extent));
        var bottomRight = toLonLat(getBottomRight(extent));

        display('left', wrapLon(bottomLeft[0]));
        display('bottom', bottomLeft[1]);
        display('right', wrapLon(topRight[0]));
        display('top', topRight[1]);

        let minY = bottomLeft[1];
        let maxY = topRight[1];
        let minX = wrapLon(bottomLeft[0]); 
        let maxX = wrapLon(topRight[0]);
        var coord = [bottomLeft[0], bottomLeft[1]];
        reverseGeocode(coord);


        // this will change to states or countiess file calculations based on zoom level
        if (Math.floor(zoomLevel) != previousZoomLevel) {
            let mapCenter = map.getView().getCenter();
            // setting zoom level <6 as states and >6 as counties
            if (zoomLevel < 6) {
                handleSelectOptions('state', zoomLevel, mapCenter, minX, minY, maxX, maxY);
            } else {
                handleSelectOptions('county', zoomLevel, mapCenter, minX, minY, maxX, maxY);

            }
            previousZoomLevel = Math.floor(zoomLevel);
        }
    }



    var initializeMapCanvas = function (mapCenter, zoomLevel) {
        $("#map").empty();

        // function for states - fill in temp, color of polygon
        var styleFunctionStates = function (feature) {
            initializeMap();
            var index = feature.getProperties()['wikipedia'].lastIndexOf('/');
            var stateName = feature.getProperties()['wikipedia'].substring(index + 1);
            var average = returnAverageTemperature(stateName);
            var color = getColorForTemp(average);
            //console.log(JSON.stringify(color) + ' color from data ' + ' ' + index + ' ' + stateName + ' ' + average);
            if (color == undefined) {
                color = {};
                color.fill = 'rgba(121, 98, 239, 0.2)'
                color.stroke = 'rgba(121, 98, 239, 1)'
            }

            return new Style({
                stroke: new Stroke({
                    color: color.stroke,
                    width: 1
                }),
                fill: new Fill({
                    color: color.fill
                })
            });
        };

        // function for counties - fill in temp, color of polygon
        var styleFunctionCounty = function (feature) {
            initializeMap();
            var countyName = feature.getProperties()['NAME'];
            var average = returnAverageTemperature(countyName);
            var color = getColorForTemp(average);
            //console.log(JSON.stringify(color) + ' color from data ' + ' ' + countyName + ' ' + average);
            if (color == undefined) {
                color = {};
                color.fill = 'rgba(121, 98, 239, 0.2)'
                color.stroke = 'rgba(121, 98, 239, 1)'
            }

            return new Style({
                stroke: new Stroke({
                    color: color.stroke,
                    width: 1
                }),
                fill: new Fill({
                    color: color.fill
                })
            });
        };

        // read states GeoJSON file
        var vectorSourceState = new VectorSource({
            features: (new GeoJSON()).readFeatures(geojsonObjectState)
        });
        // read counties GeoJson file
        var vectorSourceStateCounty = new VectorSource({
            features: (new GeoJSON()).readFeatures(geojsonObjectCounty)
        });

        var styleFunction = selection == 'state' ? styleFunctionStates : styleFunctionCounty;
        var vectorSource = selection == 'state' ? vectorSourceState : vectorSourceStateCounty;
        var vectorLayer = new VectorLayer({
            source: vectorSource,
            style: styleFunction
        });

        var map = new Map({
            layers: [
                new TileLayer({
                    source: new OSM()
                }),
                vectorLayer
            ],
            target: 'map',
            view: new View({
                center: mapCenter ? mapCenter : [-10997148, 4569099], //[39.681298, -98.544965],
                zoom: zoomLevel ? zoomLevel : 6
            })
        });
        map.on('moveend', onMoveEnd);
    }

    initializeMapCanvas(undefined, 3.5);

    var addParam = function (url, name, value) {
        url = url + name + '=' + value + '&';
        return url;
    }

    var handleSelectOptions = function (selectedValue, zoomLevel, mapCenter, minX, minY, maxX, maxY) {
        let url = 'http://localhost:12229/data/?';
        url = addParam(url, 'minX', minX);
        url = addParam(url, 'minY', minY); 
        url = addParam(url, 'maxX', maxX);
        url = addParam(url, 'maxY', maxY);
        
        
        let startDate = $('#StartDate').val();
        let endDate = $('#EndDate').val();
        console.log(startDate);
        let stDate;
        if(!startDate){
            stDate = new Date();
        }else{
            stDate = new Date(startDate);
        }

        let enDate;
        if(!endDate){
            enDate = new Date();
        }else{
            enDate = new Date(endDate);
        }
        
        

        let stDtStr = stDate.getFullYear()+"."+stDate.getMonth()+"."+stDate.getDate();
        let enDtStr = enDate.getFullYear()+"."+enDate.getMonth()+"."+enDate.getDate();

        url = addParam(url, 'startDate', stDtStr);
        url = addParam(url, 'endDate', enDtStr);
        url = addParam(url, 'query', 'allpolys');
        if (selectedValue === 'county') {

            url = addParam(url, 'datafile', 'counties');
            url = addParam(url, 'basePath', '../data/WGS84_boundaries/'); 
            url = addParam(url, 'vectorFile', '/Users/husnasayedi/Documents/UCR/MastersProject/Satellite-Data-Server/data/WGS84_boundaries/us_counties.shp');
            url = addParam(url, 'rasterFile', 'glc2000.tif');

            selection = 'county';

        } else if (selectedValue === 'state') {

            url = addParam(url, 'vectorFile', '/Users/husnasayedi/Documents/UCR/MastersProject/Satellite-Data-Server/data/WGS84_boundaries/us_states.shp');
            url = addParam(url, 'rasterFile', 'glc2000.tif');

            selection = 'state';
        }
               
        console.log(url);
        $.get(url, function (data, status) {
            console.log("Data: " + data + "\nStatus: " + status);
            resObj = JSON.parse(data);
            //alert(data + ' from server ' );

            initializeMapCanvas(mapCenter, zoomLevel);
        })
    }

    var errorFn = function (err) {
        console.log(err);
    }
    var success = function (data, status) {
        console.log("Data: " + data + "\nStatus: " + status);
        resObj = JSON.parse(data);
        //alert(data + ' from server ' );

        initializeMapCanvas(mapCenter, zoomLevel);
    }

    // get value of the selected option
    $('#optionState').on('change', function () {

        if (this.value === 'county') {
            handleSelectOptions('county');

        } else if (this.value === 'state') {
            handleSelectOptions('state');
        }
    });

    let mapCenter = map.getView().getCenter();
    //var mapCen = [-10997148, 4569099] //[39.681298, -98.544965];
    handleSelectOptions('state', 6, mapCenter);

});