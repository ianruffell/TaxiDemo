import json
import ipyleaflet as L
from ipyleaflet import Map, GeoJSON, Choropleth, LayersControl
from htmltools import css
from shiny import App, reactive, render, ui
from shinywidgets import output_widget, reactive_read, register_widget
from pygridgain import Client
import geopandas, pandas as pd, numpy as np
from branca.colormap import linear
import matplotlib.pyplot as plt

SAMPLE_PERIOD = 5
CAR_QUERY = 'SELECT LOCATIONID, count(*) FROM CAR GROUP By LOCATIONID'
STATS_QUERY = 'SELECT HOUR(PICKUP_DATETIME) AS HOUR, round(avg(BASE_PASSENGER_FARE), 2) AS AVG_FARE, round(avg(TIPS), 2) AS AVG_TIP, round(avg(TRIP_MILES / TRIP_TIME * 60 * 60)) AS MPH FROM TRIP GROUP BY HOUR(PICKUP_DATETIME)'
width = 0.35 

app_ui = ui.page_fluid(
    ui.column(6, ui.output_plot("p")),
    ui.column(6, output_widget("map")),
)

client = Client()
client.connect('127.0.0.1', 10800)

@reactive.Calc
def cars_current():
    reactive.invalidate_later(SAMPLE_PERIOD)
    
    carLocs = {}
    with client.sql(CAR_QUERY) as cursor:
        for row in cursor:
            carLocs[str(row[0])] = float(row[1])
            
    for i in range(1,263):
        x = carLocs.get(str(i))
        if x == None:
            carLocs[str(i)] = float(0)
    return carLocs

@reactive.Calc
def stats_current():
    reactive.invalidate_later(SAMPLE_PERIOD)

    stats = [[],[],[],[]]
    with client.sql(STATS_QUERY) as cursor:
        for row in cursor:
            stats[0].append(str(row[0]))
            for i in range(1, 4):
                stats[i].append(row[i])
    print(stats)
    return stats

def server(input, output, session):
    # Initialize and display when the session starts (1)
    map = Map(center=(40.7224021,-73.8138549), zoom=10, scroll_wheel_zoom=True)
    # Add a distance scale
    map.add_control(L.leaflet.ScaleControl(position="bottomleft"))
    register_widget("map", map)
    map.add_control(LayersControl())
    
    with open('NYC_Taxi_Zones.geojson', 'r') as f:
        zones = json.load(f)
    
    i=1
    for x in zones['features']:
        x['id'] = str(i)
        i += 1

    car_data = reactive.Value(None)
    stats_data = reactive.Value(None)
        
    @reactive.Effect
    def collect_car_data():
        car_data = cars_current()
        geo_data = Choropleth(
            geo_data=zones,
            choro_data=car_data,
            colormap=linear.YlOrRd_06,
            style={'fillOpacity': 1.0, "color":"black"},
            key_on='id',
            name = 'Taxi Zones')
        map.add_layer(geo_data)
        
    @reactive.Effect
    def collect_stats():
        stats_data.set(stats_current())        

    # Everytime the map's bounds change, update the output message (3)
    @output
    @render.ui
    def map_bounds():
        center = reactive_read(map, "center")
        if len(center) == 0:
            return

        lat = round(center[0], 4)
        lon = (center[1] + 180) % 360 - 180
        lon = round(lon, 4)

        return ui.p(f"Latitude: {lat}", ui.br(), f"Longitude: {lon}")
    
    @output
    @render.plot
    def p():
        fig, ax = plt.subplots()
        ax.bar(stats_data()[0], stats_data()[1], width, label='Average Fare')
        ax.bar(stats_data()[0], stats_data()[3], width, label='Average MPH')
        ax.bar(stats_data()[0], stats_data()[2], width, label='Average Tip')
        
        ax.set_ylabel('$')
        ax.set_title('NYC Taxi Trip Statistics')
        ax.legend()
        
        return fig

app = App(app_ui, server)