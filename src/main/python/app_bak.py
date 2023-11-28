import json
import ipyleaflet as L
from ipyleaflet import Map, GeoJSON, Choropleth, LayersControl
from htmltools import css
from shiny import App, reactive, render, ui
from shinywidgets import output_widget, reactive_read, register_widget
from pygridgain import Client
import geopandas, pandas as pd, numpy as np
from branca.colormap import linear

SAMPLE_PERIOD = 3

app_ui = ui.page_fluid(
    ui.div(
        ui.input_slider("zoom", "Map zoom level", value=10, min=1, max=18),
        ui.output_ui("map_bounds"),
        style=css(
            display="flex", justify_content="center", align_items="center", gap="2rem"
        ),
    ),
    output_widget("map"),
)

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
        #print(x)
        x['id'] = str(i)
        i += 1

    client = Client()
    query = 'SELECT LOCATIONID, count(*) FROM CAR GROUP By LOCATIONID'
    client.connect('127.0.0.1', 10800)
    
    carLocs = {}
    with client.sql(query) as cursor:
        for row in cursor:
            carLocs[str(row[0])] = float(row[1])
    
    for i in range(1,263):
        x = carLocs.get(str(i))
        if x == None:
            carLocs[str(i)] = float(0)
        
    geo_data = Choropleth(
        geo_data=zones,
        choro_data=carLocs,
        colormap=linear.YlOrRd_06,
        style={'fillOpacity': 1.0, "color":"black"},
        key_on='id',
        name = 'Taxi Zones')
    map.add_layer(geo_data)
    
    # When the slider changes, update the map's zoom attribute (2)
    @reactive.Effect
    def _():
        map.zoom = input.zoom()

    # When zooming directly on the map, update the slider's value (2 and 3)
    @reactive.Effect
    def _():
        ui.update_slider("zoom", value=reactive_read(map, "zoom"))

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


app = App(app_ui, server)